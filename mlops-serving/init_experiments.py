import mlflow

mlflow.set_tracking_uri("http://127.0.0.1:5000")

for name in ["fraud-benchmark", "fraud-uncertainty", "fraud-champion"]:
    if mlflow.get_experiment_by_name(name) is None:
        mlflow.create_experiment(name)

mlflow.set_experiment("fraud-benchmark")
with mlflow.start_run(run_name="smoke-test"):
    mlflow.log_param("test", 1)
    mlflow.log_metric("roc_auc", 0.5)

print("OK")