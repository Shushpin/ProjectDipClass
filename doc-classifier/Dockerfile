# === СТАДІЯ 1: Збірка (Build) ===
# Використовуємо образ з повним JDK 21 та Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. Спочатку копіюємо ТІЛЬКИ pom.xml і завантажуємо залежності.
# Це робиться для кешування шарів. Якщо змінити код у src/,
# Docker не буде перекачувати половину інтернету (всі .jar бібліотеки Spring).
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Копіюємо вихідний код і збираємо фінальний .jar файл без запуску тестів
COPY src ./src
RUN mvn clean package -DskipTests

# === СТАДІЯ 2: Виконання (Run) ===
# Використовуємо легкий JRE (тільки для запуску).
# Беремо jammy (Ubuntu), бо для Apache Tika (парсинг документів) часто потрібні системні бібліотеки
FROM eclipse-temurin:21-jre-jammy

# 3. Створюємо непривілейованого користувача (Security Best Practice)
RUN useradd -m -r spring_user

WORKDIR /app

# 4. Створюємо папку для завантажених файлів і даємо права нашому юзеру
# Це важливо, бо Spring намагатиметься зберігати туди PDF/Docx
RUN mkdir -p /app/uploads && chown -R spring_user:spring_user /app

# 5. Копіюємо зібраний .jar файл з першої стадії (builder)
COPY --from=builder /app/target/*.jar app.jar
RUN chown spring_user:spring_user app.jar

# 6. Переходимо на безпечного користувача
USER spring_user

# 7. Відкриваємо порт
EXPOSE 8080

# 8. Команда для запуску
CMD ["java", "-jar", "app.jar"]