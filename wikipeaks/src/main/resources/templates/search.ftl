<#include "header.ftl">

<#list context.pages>
<h2>Pages:</h2>
<ul>
    <#items as page>
        <li><a href="/wiki/${page}">${page}</a></li>
    </#items>
</ul>
<#else>
<p>The wiki is currently empty!</p>
</#list>

<p>
    <#list context.pageContentParts as pageContentPart>
        <p>
            page: <a href="/wiki/${pageContentPart.pageName}">${pageContentPart.pageName}</a> <br/>
            hint: ${pageContentPart.contentPart}
        </p>
    <#else>
        <p>
            The search did not return any result... (maybe you should broaden your search criteria)
        </p>
    </#list>

</p>




<#include "footer.ftl">