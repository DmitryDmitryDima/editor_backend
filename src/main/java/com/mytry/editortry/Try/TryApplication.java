package com.mytry.editortry.Try;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;



/*

Идеи:

 - Комбинация java parser и ai для системы подсказок
 - Пока что ai применяем для импортов и специфических задач, генерации примеров (дополняем по мере изучения)
 - Можно сделать голосовой ввод, взаимодействующий только с ai, при этом должна быть система отката кода, общая для всего проекта
и специфичная для голосовых ai правок
 - Реализация системы пользователей, с регистрацией и авторизацией - workspace будет походить на файловую систему,
 где можно будет создавать несколько проектов со сложной структурой, при этом все это будет адаптировано под телефон
 - code suggestion, соответственно, будет опираться на файлы и базу данных, а не на состояние кода "в полете" и временный файл
 - Контейнеризация сервера с компилятором и ai (vLLM и линукс - подбираем самую быструю комбинацию модели и движка)
 Возможно запуск нескольких серверов (images - Docker и Kubernetes). Хостинг на сторонних серверах и, соответсвенно,
 открытый доступ к приложению. Разработка системы безопасного запуска стороннего кода.
 - В перспективе - несколько языков, фреймворки и мультиплеер


 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class TryApplication {

	public static void main(String[] args) {
		SpringApplication.run(TryApplication.class, args);
	}

}
