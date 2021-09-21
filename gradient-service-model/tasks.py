from invoke import task

project_name = "gradient-service-model"


@task
def install(c):
    print("[gradient-service-model] Installing")

    c.run(f"conda env create --force -f requirements.yaml")

    print("[gradient-service-model] Installing done")


@task
def build(c):
    print("[gradient-service-model] Building")

    print("[gradient-service-model] Build done")


@task
def test(c):
    print("[gradient-service-model] Testing")

    c.run(f"conda run --live-stream -n {project_name} python -m pytest tests")

    print("[gradient-service-model] Test done")


@task
def publish(c):
    pass


@task(default=True)
def run(c):
    from gradient.model import bootstrap
    bootstrap.main()
