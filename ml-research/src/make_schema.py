"""Finalise le schéma de features (P2.2, dernière sous-étape).

1. Vérifie que artifacts/feature_schema.json est cohérent avec les décisions de P2.2.
2. Génère un VRAI JSON Schema (draft 2020-12) : artifacts/feature_input.schema.json
   -> il décrit le corps JSON que P3 (FastAPI) doit accepter.
3. Génère un exemple valide : artifacts/feature_input.example.json
4. Teste le schéma sur un exemple valide et sur des exemples invalides.

Règle de versionnage (semver) : MAJOR si on ajoute/retire/renomme une colonne ou si une
transformation change ; MINOR pour un ajout rétro-compatible ; PATCH pour la documentation.

Usage (depuis ml-research/, avec le .venv actif) :  python src/make_schema.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "artifacts"
V_COLS = [f"V{i}" for i in range(1, 29)]
EXPECTED_COLUMNS = V_COLS + ["Amount"]

ok = True


def check(label, condition, detail=""):
    global ok
    ok = ok and bool(condition)
    print(("OK    " if condition else "ERREUR"), "-", label, detail)


# ---------------------------------------------------------------- 1. vérifications
src = ART / "feature_schema.json"
if not src.exists():
    sys.exit(f"Fichier introuvable : {src}\nLance d'abord la section 7 du notebook P2.2.")

fs = json.loads(src.read_text(encoding="utf-8"))
print(f"Lecture de {src.name} (schema_version {fs.get('schema_version')})\n")

check("Time absente des colonnes d'entrée", "Time" not in fs["input_columns"])
check("Colonnes = V1..V28 + Amount", fs["input_columns"] == EXPECTED_COLUMNS,
      f"({len(fs['input_columns'])} colonnes)")
check("Stratégie de déséquilibre = none", fs.get("imbalance_strategy") == "none",
      f"(valeur : {fs.get('imbalance_strategy')})")
check("Graine = 42", fs.get("seed") == 42)

splits = fs.get("splits", {})
check("Trois lots présents", set(splits) == {"train", "cal", "test"})
if set(splits) == {"train", "cal", "test"}:
    total_rows = sum(s["rows"] for s in splits.values())
    total_frauds = sum(s["frauds"] for s in splits.values())
    check("Total = 283 726 lignes", total_rows == 283726, f"({total_rows})")
    check("Total = 473 fraudes", total_frauds == 473, f"({total_frauds})")
    for name, s in splits.items():
        print(f"         {name:<5}: {s['rows']:>7} lignes, {s['frauds']:>4} fraudes")

if not ok:
    sys.exit("\nLe schéma de P2.2 est incohérent : corrige-le (relance le notebook) avant de continuer.")

# ---------------------------------------------------------------- 2. JSON Schema
version = fs["schema_version"]
properties = {
    c: {"type": "number", "description": "Composante PCA anonymisée (variable d'origine inconnue)."}
    for c in V_COLS
}
properties["Amount"] = {
    "type": "number", "minimum": 0,
    "description": "Montant de la transaction en euros (brut, non transformé). "
                   "Le pipeline applique lui-même log(1 + x) puis la standardisation.",
}

json_schema = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": f"https://fintech-mlops.local/schemas/fraud-features/{version}",
    "title": "Features d'entrée du modèle de détection de fraude",
    "description": "Colonnes BRUTES attendues par le pipeline (prétraitement + modèle). "
                   "Time est volontairement exclue (décision P2.2).",
    "x-schema-version": version,
    "type": "object",
    "properties": properties,
    "required": EXPECTED_COLUMNS,
    "additionalProperties": False,
}
(ART / "feature_input.schema.json").write_text(
    json.dumps(json_schema, indent=2, ensure_ascii=False), encoding="utf-8")

example = {c: 0.0 for c in V_COLS}
example["Amount"] = 12.4
(ART / "feature_input.example.json").write_text(
    json.dumps(example, indent=2), encoding="utf-8")
print("\nFichiers écrits : feature_input.schema.json, feature_input.example.json")

# ---------------------------------------------------------------- 3. tests du schéma
try:
    from jsonschema import Draft202012Validator
except ImportError:
    sys.exit("\nInstalle jsonschema pour tester le schéma :  pip install jsonschema")

Draft202012Validator.check_schema(json_schema)
validator = Draft202012Validator(json_schema)


def errors_of(payload):
    return [e.message for e in validator.iter_errors(payload)]


print("\nTests de validation :")
check("Exemple valide accepté", not errors_of(example))

missing = {k: v for k, v in example.items() if k != "V14"}
check("Colonne manquante (V14) refusée", errors_of(missing))

negative = {**example, "Amount": -5}
check("Montant négatif refusé", errors_of(negative))

wrong_type = {**example, "V3": "abc"}
check("Valeur non numérique refusée", errors_of(wrong_type))

with_time = {**example, "Time": 1000}
check("Colonne Time refusée (non prévue)", errors_of(with_time))

print("\n" + ("Tout est cohérent." if ok else "Des vérifications ont échoué."))
sys.exit(0 if ok else 1)