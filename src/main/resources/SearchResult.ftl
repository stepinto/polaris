<html>
    <body>
        <h1>Polaris Code Search></h1>
        <form>
            <input type="text" name="q" />
            <input type="submit" value="search" />
        </form>
        <ul>
            <li>
                <#list results as result>
                    <p>${result.summary}</p>
                    <p>${result.filename}</p>
                </#list>
            </li>
        </ul>
    </body>
</html>