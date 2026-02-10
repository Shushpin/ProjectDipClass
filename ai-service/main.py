from flask import Flask, request, jsonify
from transformers import pipeline

app = Flask(__name__)

print("Завантаження моделі AI... Це може зайняти хвилину при першому запуску.")

# Використовуємо мультимовну модель, яка розуміє українську
# pipeline("zero-shot-classification") дозволяє класифікувати без тренування
classifier = pipeline("zero-shot-classification", model="MoritzLaurer/mDeBERTa-v3-base-mnli-xnli")

print("Модель завантажена і готова до роботи!")

@app.route('/classify', methods=['POST'])
def classify_document():
    # Отримуємо JSON від Java
    data = request.json
    text = data.get('text', '')

    if not text:
        return jsonify({"error": "No text provided"}), 400

    # Список категорій, з яких AI має вибрати (можна змінювати!)
    # Змініть цей рядок у main.py
    candidate_labels = [
        "Навчальні матеріали та лекції",  # Більш конкретно
        "Юридичний договір та наказ",     # Уточнення для юр. документів
        "Бухгалтерія та рахунки",         # Замість "Фінансовий звіт" (більш приземлено)
        "Технічна документація",
        "Художня література та поезія"    # Додали "поезія" для Симоненка
    ]

    # Запускаємо класифікацію
    output = classifier(text, candidate_labels, multi_label=False)

    # Отримуємо найкращий результат
    best_label = output['labels'][0]
    confidence = output['scores'][0]

    print(f"Оброблено документ. Результат: {best_label} ({confidence:.2f})")

    # Повертаємо відповідь назад у Java
    return jsonify({
        "category": best_label,
        "confidence": confidence
    })

if __name__ == '__main__':
    # Запускаємо сервер на порту 5000
    app.run(port=5000, debug=True)