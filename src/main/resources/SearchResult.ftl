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
                    <p><font color="green">${result.filename}</font></p>
                    <p><pre>${result.summary}</pre></p>
                </li>
            </#list>
        </ul>
    </body>
</html>
