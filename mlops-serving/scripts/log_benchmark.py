"""P3.2 : enregistre le benchmark de P2 (CSV) dans MLflow, un run par algorithme."""
import os
from pathlib import Path

import mlflow
import pandas as pd

TRACKING_URI = os.getenv("MLFLOW_TRACKING_URI", "http://127.0.0.1:5000")
EXPERIMENT = "fraud-benchmark"
BENCH = Path(__file__).resolve().parents[2] / "ml-research" / "artifacts" / "benchmark"

ALGOS = {
    "logreg": "Regression logistique",
    "rf": "Random Forest",
    "gb": "XGBoost",
    "svm": "SVM (RBF)",
    "mlp": "MLP",
}
METRICS = ["pr_auc", "roc_auc", "f1_at_0_5", "mcc_at_0_5", "brier", "train_time_s"]

df = pd.read_csv(BENCH / "cv_results.csv")
algo_col, fold_col = df.columns[0], df.columns[1]
metric_cols = list(df.columns[2:8])

mlflow.set_tracking_uri(TRACKING_URI)
mlflow.set_experiment(EXPERIMENT)

for key, label in ALGOS.items():
    sub = df[df[algo_col] == key].sort_values(fold_col)
    if sub.empty:
        print("Algo absent du CSV :", key)
        continue
    with mlflow.start_run(run_name=f"benchmark-{key}"):
        mlflow.set_tags({"source": "P2.4", "algo_label": label})
        mlflow.log_params({"algorithm": key, "n_folds": len(sub), "n_seeds": 1})
        for name, col in zip(METRICS, metric_cols):
            mlflow.log_metric(f"{name}_mean", float(sub[col].mean()))
            mlflow.log_metric(f"{name}_std", float(sub[col].std()))
            for _, row in sub.iterrows():
                mlflow.log_metric(name, float(row[col]), step=int(row[fold_col]))
        mlflow.log_artifact(str(BENCH / "cv_results.csv"))
        mlflow.log_artifact(str(BENCH / "summary_cv.csv"))
        print("OK", key, "-> PR-AUC moyen", round(float(sub[metric_cols[0]].mean()), 4))