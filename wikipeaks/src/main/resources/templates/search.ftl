<#include "header.ftl">

<div class="row">
    <div class="col-md-12 mt-1">
      <span class="float-xs-right">
        <a class="btn btn-outline-primary" href="/" role="button" aria-pressed="true">Home</a>
    </div>
</div>

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