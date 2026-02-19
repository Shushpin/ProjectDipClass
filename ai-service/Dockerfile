# Використовуємо офіційний легкий образ Python
FROM python:3.11-slim

# 1. Створюємо непривілейованого користувача (Security Best Practice)
RUN useradd -m -r ai_user

# 2. Встановлюємо робочу директорію
WORKDIR /app

# 3. Вказуємо директорію для кешу HuggingFace (щоб модель зберігалася тут)
ENV HF_HOME=/app/.cache/huggingface
RUN mkdir -p $HF_HOME && chown -R ai_user:ai_user /app

# 4. Копіюємо список залежностей
COPY requirements.txt .

# 5. Встановлюємо залежності (без кешу самого pip, щоб зменшити вагу образу)
RUN pip install --no-cache-dir -r requirements.txt

# 6. ПЕРЕХОДИМО НА БЕЗПЕЧНОГО КОРИСТУВАЧА
USER ai_user

# 7. "ЗАПІКАЄМО" МОДЕЛЬ В ОБРАЗ
# Ця команда виконається ПІД ЧАС ЗБІРКИ образу. Вона завантажить DeBERTa і збереже в HF_HOME.
RUN python -c "from transformers import pipeline; pipeline('zero-shot-classification', model='MoritzLaurer/mDeBERTa-v3-base-mnli-xnli')"

# 8. Копіюємо наш код у контейнер
COPY main.py .

# 9. Відкриваємо порт
EXPOSE 5000

# 10. Команда для запуску Flask-сервера
CMD ["flask", "--app", "main", "run", "--host=0.0.0.0", "--port=5000"]