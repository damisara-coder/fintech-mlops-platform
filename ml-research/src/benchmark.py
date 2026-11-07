"""Protocole de benchmark (P2.4), réutilisé par P2.5 (validation statistique) et P2.7 (champion).

Principes :
- tous les modèles reçoivent exactement les mêmes lots (data_prep.make_splits) et le même prétraitement ;
- toute transformation est apprise dans chaque pli d'entraînement (pas de fuite) ;
- le lot de TEST n'est jamais utilisé ici.
"""
import json
import re
import time
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.base import BaseEstimator, ClassifierMixin, clone
from sklearn.ensemble import HistGradientBoostingClassifier, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (average_precision_score, brier_score_loss, f1_score,
                             matthews_corrcoef, roc_auc_score)
from sklearn.model_selection import RepeatedStratifiedKFold
from sklearn.neural_network import MLPClassifier
from sklearn.pipeline import Pipeline
from sklearn.svm import SVC

from src.data_prep import SEED, build_preprocessor

MODELS = ["logreg", "rf", "gb", "svm", "mlp"]
METRICS = ["pr_auc", "roc_auc", "f1", "mcc", "brier"]
LABELS = {"pr_auc": "PR-AUC", "roc_auc": "ROC-AUC", "f1": "F1 (seuil 0,5)",
          "mcc": "MCC (seuil 0,5)", "brier": "Brier", "fit_time_s": "Entraînement (s)"}


def display_names(gb_kind):
    gb = "XGBoost" if gb_kind == "xgboost" else "Gradient Boosting (HistGB)"
    return {"logreg": "Régression logistique", "rf": "Random Forest", "gb": gb,
            "svm": "SVM (RBF)", "mlp": "MLP"}


# --------------------------------------------------------------------------- SVM : sous-échantillon
class UndersampledClassifier(ClassifierMixin, BaseEstimator):
    """Sous-échantillonne la classe majoritaire À L'ENTRAÎNEMENT (utile pour le SVM, trop lent sur
    170 000 lignes) puis corrige les probabilités du biais introduit :
    p = beta * ps / (beta * ps - ps + 1), beta = part des légitimes conservées
    (Dal Pozzolo et al., 2015 - l'article de référence du dataset)."""

    def __init__(self, estimator=None, n_majority=15000, random_state=0, correct_prior=True):
        self.estimator = estimator
        self.n_majority = n_majority
        self.random_state = random_state
        self.correct_prior = correct_prior

    def fit(self, X, y):
        X, y = np.asarray(X), np.asarray(y)
        rng = np.random.default_rng(self.random_state)
        idx_pos, idx_neg = np.flatnonzero(y == 1), np.flatnonzero(y == 0)
        kept = idx_neg if len(idx_neg) <= self.n_majority else rng.choice(idx_neg, self.n_majority, replace=False)
        self.beta_ = len(kept) / len(idx_neg)
        idx = np.concatenate([idx_pos, kept])
        self.estimator_ = clone(self.estimator).fit(X[idx], y[idx])
        self.classes_ = self.estimator_.classes_
        return self

    def predict_proba(self, X):
        p = self.estimator_.predict_proba(np.asarray(X))
        if self.correct_prior and self.beta_ < 1:
            ps = p[:, 1]
            p1 = self.beta_ * ps / (self.beta_ * ps - ps + 1)
            p = np.column_stack([1 - p1, p1])
        return p

    def predict(self, X):
        return (self.predict_proba(X)[:, 1] >= 0.5).astype(int)


# --------------------------------------------------------------------------- modèles
def make_classifier(key, gb_kind="hist", xgb_params=None, seed=SEED, svm_n_majority=15000, **params):
    """Réglages raisonnables et identiques pour tous ; `params` surcharge les réglages (réglage léger)."""
    params = dict(params)
    if key == "logreg":
        return LogisticRegression(**{"max_iter": 1000, **params})
    if key == "rf":
        return RandomForestClassifier(**{"n_estimators": 100, "n_jobs": -1, "random_state": seed, **params})
    if key == "gb":
        if gb_kind == "xgboost":
            from xgboost import XGBClassifier
            return XGBClassifier(**{"n_estimators": 200, "learning_rate": 0.1, "max_depth": 6,
                                    "eval_metric": "aucpr", "n_jobs": -1, "random_state": seed,
                                    **(xgb_params or {}), **params})
        return HistGradientBoostingClassifier(**{"max_iter": 200, "learning_rate": 0.1,
                                                 "random_state": seed, **params})
    if key == "svm":
        n_major = params.pop("n_majority", svm_n_majority)
        svc = SVC(**{"kernel": "rbf", "probability": True, "random_state": seed, **params})
        return UndersampledClassifier(svc, n_majority=n_major, random_state=seed)
    if key == "mlp":
        return MLPClassifier(**{"hidden_layer_sizes": (64, 64), "batch_size": 512, "max_iter": 100,
                                "early_stopping": True, "n_iter_no_change": 8,
                                "random_state": seed, **params})
    raise ValueError(f"Modèle inconnu : {key}")


def make_pipeline(key, **kw):
    return Pipeline([("prep", build_preprocessor(include_time=False)), ("clf", make_classifier(key, **kw))])


# --------------------------------------------------------------------------- XGBoost : le diagnostic
XGB_VARIANTS = {
    "défaut": {},
    "base_score=0.5": {"base_score": 0.5},
    "tree_method=approx": {"tree_method": "approx"},
    "min_child_weight=0.01": {"min_child_weight": 0.01},
    "base_score=0.5 + min_child_weight=0.01": {"base_score": 0.5, "min_child_weight": 0.01},
}


def check_xgboost(Z_train, y_train, Z_cal, y_cal, seed=SEED):
    """Teste XGBoost et quelques variantes. Un modèle est jugé sain s'il produit des probabilités
    variées (> 100 valeurs distinctes) et une PR-AUC > 0,3 sur le lot de calibration.
    Retourne (ok, paramètres_qui_marchent, rapport, info)."""
    try:
        import xgboost
        from xgboost import XGBClassifier
    except ImportError:
        return False, {}, pd.DataFrame(), "xgboost n'est pas installé : on utilisera HistGradientBoosting."
    rows, found, chosen = [], False, {}
    for name, params in XGB_VARIANTS.items():
        try:
            clf = XGBClassifier(n_estimators=200, learning_rate=0.1, max_depth=6, eval_metric="aucpr",
                                n_jobs=-1, random_state=seed, **params)
            clf.fit(Z_train, np.asarray(y_train))
            p = clf.predict_proba(Z_cal)[:, 1]
            pr, distinct = float(average_precision_score(y_cal, p)), int(np.unique(p).size)
            ok = distinct > 100 and pr > 0.3
            rows.append({"variante": name, "PR-AUC": round(pr, 4), "valeurs distinctes": distinct, "sain": ok})
            if ok and not found:
                found, chosen = True, params
        except Exception as exc:  # noqa: BLE001
            rows.append({"variante": name, "PR-AUC": np.nan, "valeurs distinctes": 0, "sain": False,
                         "erreur": str(exc)[:80]})
    return found, chosen, pd.DataFrame(rows), f"xgboost {xgboost.__version__}"


# --------------------------------------------------------------------------- métriques et validation croisée
def compute_metrics(y_true, proba):
    y_true = np.asarray(y_true)
    pred = (np.asarray(proba) >= 0.5).astype(int)
    return {
        "pr_auc": float(average_precision_score(y_true, proba)),
        "roc_auc": float(roc_auc_score(y_true, proba)),
        "f1": float(f1_score(y_true, pred, zero_division=0)),
        "mcc": float(matthews_corrcoef(y_true, pred)),
        "brier": float(brier_score_loss(y_true, proba)),
    }


def params_tag(params):
    return re.sub(r"[^0-9a-zA-Z.]+", "-", str(sorted(params.items()))).strip("-")


def _cache_tag(key, kw):
    tag = {"gb": kw.get("gb_kind", "hist"), "svm": f"n{kw.get('svm_n_majority', 15000)}"}.get(key, "base")
    extra = {k: v for k, v in kw.items() if k not in ("gb_kind", "xgb_params", "svm_n_majority")}
    return f"{tag}_{params_tag(extra)}" if extra else tag


def cross_validate_model(key, X, y, n_splits=5, n_repeats=1, seed=SEED, cache_dir=None, force=False, **kw):
    """Validation croisée stratifiée. Pour P2.5 : n_splits=10, n_repeats=5.
    Les résultats sont mis en cache (un fichier par modèle) : un plantage ne fait pas tout perdre."""
    cache = None
    if cache_dir is not None:
        Path(cache_dir).mkdir(parents=True, exist_ok=True)
        cache = Path(cache_dir) / f"cv_{key}_{_cache_tag(key, kw)}_{n_splits}x{n_repeats}.csv"
        if cache.exists() and not force:
            print(f"  (résultats déjà calculés, chargés depuis {cache.name})")
            return pd.read_csv(cache)
    y = np.asarray(y)
    cv = RepeatedStratifiedKFold(n_splits=n_splits, n_repeats=n_repeats, random_state=seed)
    total, rows = n_splits * n_repeats, []
    for i, (tr, va) in enumerate(cv.split(X, y)):
        pipe = make_pipeline(key, **kw)
        t0 = time.time()
        pipe.fit(X.iloc[tr], y[tr])
        fit_s = time.time() - t0
        proba = pipe.predict_proba(X.iloc[va])[:, 1]
        m = compute_metrics(y[va], proba)
        rows.append({"model": key, "split": i, **m, "fit_time_s": fit_s})
        print(f"  pli {i + 1}/{total} : PR-AUC = {m['pr_auc']:.4f} ({fit_s:.0f} s)")
    res = pd.DataFrame(rows)
    if cache is not None:
        res.to_csv(cache, index=False)
    return res


def summarize(cv_df, names):
    """Tableau « moyenne ± écart-type » trié par PR-AUC décroissante."""
    agg = cv_df.groupby("model")[METRICS + ["fit_time_s"]].agg(["mean", "std"])
    order = agg[("pr_auc", "mean")].sort_values(ascending=False).index
    table = pd.DataFrame(index=[names[m] for m in order])
    for c in METRICS:
        f = ".6f" if c == "brier" else ".4f"   # le Brier est très petit : 6 décimales
        table[LABELS[c]] = [f"{agg.loc[m, (c, 'mean')]:{f}} ± {agg.loc[m, (c, 'std')]:{f}}" for m in order]
    table[LABELS["fit_time_s"]] = [f"{agg.loc[m, ('fit_time_s', 'mean')]:.1f}" for m in order]
    return table, agg


def latency_ms(pipeline, X_rows, n=100):
    """Latence d'inférence pour UNE transaction (comme en production, donc sans parallélisme)."""
    clf = pipeline.named_steps["clf"]
    for target in (clf, getattr(clf, "estimator", None)):
        if target is not None and "n_jobs" in target.get_params():
            target.set_params(n_jobs=1)
    rows = [X_rows.iloc[[i]] for i in range(min(n, len(X_rows)))]
    pipeline.predict_proba(rows[0])  # échauffement
    times = []
    for r in rows:
        t0 = time.perf_counter()
        pipeline.predict_proba(r)
        times.append((time.perf_counter() - t0) * 1000)
    return {"latence médiane (ms)": round(float(np.median(times)), 2),
            "latence p95 (ms)": round(float(np.percentile(times, 95)), 2)}


def save_best_params(path, best):
    Path(path).write_text(json.dumps(best, indent=2, ensure_ascii=False), encoding="utf-8")


def load_best_params(path):
    p = Path(path)
    if not p.exists():
        return {}
    best = json.loads(p.read_text(encoding="utf-8"))
    for params in best.values():
        if "hidden_layer_sizes" in params:
            params["hidden_layer_sizes"] = tuple(params["hidden_layer_sizes"])
    return best
