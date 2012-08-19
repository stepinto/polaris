<html>
    <body>
        <h1>Polaris Code Search</h1>
        <form>
            <input type="text" name="q" />
            <input type="submit" value="search" />
        </form>
        <hr />
        <ul>
            <#list results as result>
                <li>
                    <p><a href="/source?doc=${result.documentId?c}">${result.filename}</a></p>
                    <p><pre>${result.summary}</pre></p>
                </li>
            </#list>
        </ul>
    </body>
</html>
