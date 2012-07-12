<#-- @ftlvariable name="" type="com.example.views.PersonView" -->
<html>
<body>
    <!-- calls getListing().get("description") and sanitizes it -->
    <h1>${listing["title"]?html}!</h1>
    <div>${listing["description"]?html}!</div>
</body>
</html>