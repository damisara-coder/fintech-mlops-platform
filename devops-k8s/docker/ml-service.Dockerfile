# ---- Etape 1 : installation des dependances ----
FROM python:3.11-slim AS build
ARG REQUIREMENTS=requirements.txt
WORKDIR /w
COPY ${REQUIREMENTS} requirements.txt
RUN --mount=type=cache,target=/root/.cache/pip pip install --default-timeout=120 --retries 10 --prefix=/install -r requirements.txt

# ---- Etape 2 : image finale ----
FROM python:3.11-slim
ENV PYTHONUNBUFFERED=1
RUN useradd --system --create-home app
WORKDIR /app
COPY --from=build /install /usr/local
COPY app ./app
USER app
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
