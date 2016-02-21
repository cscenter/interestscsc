Модуль posttongram приводит посты из базы данных к списку униграмм, прогоняя тексты последовательно через mystem и tomita.

Инструкция по применению:
1. Скачать себе ветку master
2. Открыть проект в Intellij Idea (так, чтобы главной папкой была interestscsc(interestscsc-master)) и, нажав правой кнопкой мышки на pom.xml выбрать +Add as Maven Project
3. Назначить SetupSDK (оно само попросится)
4. Скачать соответствующий операционной системе файл mystem (Версия 3.0) https://tech.yandex.ru/mystem/ и распаковать в src/main/resources/posttongram/mystem
5. Скачать соответствующий операционной системе файл tomita https://tech.yandex.ru/tomita/ и распаковать в src/main/resources/posttongram/tomita
6. Попробовать запустить src/test/java/com/interestscsc/posttongram/test/NormalizatorTest.java

Возможные трудности:
Может неправильно определить имена исполняемых файлов mystem и tomita, потому что они зависят от операционной системы, а у меня, к сожалению, нет возможности протестить их все, поэтому в этом случае их можно указать явно в параметрах (только названия, без директорий)

    private static final String TOMITA_FILENAME = "";
    private static final String MYSTEM_FILENAME = "";
(например, private static final String TOMITA_FILENAME = "tomita-linux64";

в src/main/java/com/interestscsc/posttongram/Normalizator.java, а потом рассказать об ошибке мне. В остальном должно работать.

Обо всех ошибках сразу сообщайте, любые комментарии к коду и структуре модуля приветствуются!
