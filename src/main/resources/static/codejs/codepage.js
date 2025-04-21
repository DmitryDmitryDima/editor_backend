

// реализация предзагрузки данных перед запуском editor


document.addEventListener("DOMContentLoaded", async ()=>{

    // Step 1 - предзагрузка данных (в частности текста кода на странице)

    // это будет храниться в файловой системе
    var textAddress = "/files/initial/"
    let initialText = "";


    try {
        // Загрузка начального текста с сервера
        const response = await fetch(textAddress);
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        const data = await response.json();
        initialText = data.text || '';
    } catch (error) {
        console.error("Ошибка при загрузке начального текста:", error);
        initialText = `// Ошибка загрузки текста\n// Проверьте подключение к серверу.`;
    }


    // Step 2 - запуск monaco editor, а также пераметров других кнопок и полей
    require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.52.2/min/vs' }});
    require(['vs/editor/editor.main'], function() {


        // !! инициализация monaco

        var editor = monaco.editor.create(document.getElementById('editor-container'), {
            value: initialText, // Initial code
            language: "java", // Set the language to Java
            theme: "vs-dark" ,// Use a dark theme

            automaticLayout: true, // Ensures editor resizes with the window

            // mobile phone optimization
            minimap: { enabled: false }, // Disable minimap
            lineNumbers: 'off',          // Hide line numbers
            fontSize: 14,                // Increase font size
            lineHeight: 20, // Adjust line height for better readability
            wordWrap: 'on', // Enable word wrap for smaller screens
            scrollBeyondLastLine: false, // Disable scrolling beyond the last line
            scrollbar: {
                vertical: 'auto',      // Hide vertical scrollbar
                horizontal: 'hidden'     // Hide horizontal scrollbar
            }

        });






        // monaco - пример фиксирования позиции курсора
        editor.onDidChangeCursorPosition(function(event) {
            var position = event.position;
            console.log("Cursor Position - Line: " + position.lineNumber + ", Column: " + position.column);
        });

        // monaco - отображаем выбранный текст
        editor.onDidChangeCursorSelection(function(event) {
            var selection = event.selection;
            var selectedText = editor.getModel().getValueInRange(selection);
            console.log("Selected Text: " + selectedText);
        });



        // monaco - обрабатываем события печати определенных символов. В частности, для предложки нас интересует точка
        monaco.languages.registerCompletionItemProvider('java', {
            triggerCharacters: ['.', '(', ' '],
            provideCompletionItems: async function(model, position) {
                var textUntilPosition = model.getValueInRange({
                    startLineNumber: position.lineNumber,
                    startColumn: 1,
                    endLineNumber: position.lineNumber,
                    endColumn: position.column
                });

                console.log("Text before cursor:", textUntilPosition);

                // если точка
                if (textUntilPosition.endsWith(".")) {
                    const objectName = textUntilPosition.slice(0, -1).trim().split(/\s+/).pop();
                    console.log(objectName);
                    // формируем предложный список посредством запроса на сервер
                    // ДАЛЕЕ ВСЕ ОПТИМИЗИРОВАТЬ, ВОЗМОЖНО ПРИСЫЛАТЬ УЖЕ ГОТОВЫЙ JSON С ПОЛЯМИ
                    // посылаем контект (код) и имя объекта перед точкой
                    // проблема - если метод возвращает объект - нужно вычислить его тип

                    var way = "/editorapi/dotSuggest/";

                    var answer = { suggestions: [] };

                    // абсолютная позиция в строке String - нужно для бэкенда
                    let absolute = editor.getModel().getOffsetAt(editor.getPosition());


                    try {
                        const response = await fetch(way, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ code: editor.getValue(),
                                object:objectName, absolute:absolute})
                        })

                        if (!response.ok) {
                            throw new Error(`HTTP error! Status: ${response.status}`);
                        }

                        const data = await response.json();

                        console.log(data);

                        data.suggestions.forEach(method=>{
                            answer.suggestions.push({
                                label: method,
                                kind: monaco.languages.CompletionItemKind.Method,
                                insertText:method+'()'
                            })
                        })

                        // шаблон готового ответа
                        var workingAnswer = {suggestions: [
                                {
                                    label: 'toString',
                                    kind: monaco.languages.CompletionItemKind.Method,
                                    insertText:'toString()'
                                },
                                {
                                    label: 'length',
                                    kind: monaco.languages.CompletionItemKind.Property,
                                    insertText: 'length'
                                },

                                {
                                    label: 'length1',
                                    kind: monaco.languages.CompletionItemKind.Property,
                                    insertText: 'length'
                                }
                            ]}

                        return answer;


                    }

                    catch (error){
                        console.error("Error fetching suggestions:", error);
                        return answer;
                    }

                    return answer;







                }

                return {

                    // несколько типов предложек
                    suggestions: [
                        { label: 'System.out.println', kind: monaco.languages.CompletionItemKind.Method },
                        { label: 'String',
                            kind: monaco.languages.CompletionItemKind.Class
                        },
                        { label: 'int', kind: monaco.languages.CompletionItemKind.Keyword }
                    ]
                };
            }
        });














        // запуск кода - кнопки и консоль

        var consoleContainer = document.getElementById('console-container');
        var runButton = document.getElementById('run-button');

        var way = "/editorapi/run/"

        // Handle the Run button click
        runButton.addEventListener('click', function() {
            // Clear the console
            consoleContainer.innerHTML = '';

            // Get the code from the editor
            var code = editor.getValue();


            fetch(way, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ info: code })
            })
                .then(response => response.json())
                .then(data => {
                    consoleContainer.innerHTML += '> Output:\n' + data.answer + '\n';
                })
                .catch(error => {
                    consoleContainer.innerHTML += '> Error: ' + error.message + '\n';
                });




        });










    });



})




