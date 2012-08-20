<html>
    <head>
        <link rel="stylesheet" type="text/css" href="/static/main.css" />
    </head>
    <body>
        <h1>Polaris Code Search</h1>
        <form>
            <input type="text" name="q" value="${query}" />
            <input type="submit" value="search" />
        </form>
        <hr />
        <p>Took ${seconds_str}s</p>
        <div class="search_result">
            <ul>
                <#list results as result>
                    <li>
                        <p><a href="/source?filename=${result.filename}">${result.filename}</a></p>
                        <p><pre>${result.summary}</pre></p>
                    </li>
                </#list>
            </ul>
        </div>
    </body>
</html>
