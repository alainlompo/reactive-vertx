<#include "header.ftl">

<div class="row">

    <div class="col-md-12 mt-1">
        <#if context.canCreatePage>
            <div class="float-xs-right">
                <form class="form-inline" action="/create" method="post">
                    <div class="form-group">
                        <input type="text" class="form-control" id="name" name="name" placeholder="New page name">
                    </div>
                    <button type="submit" class="btn btn-primary" id="createButton">Create</button>
                </form>
            </div>
        </#if>

        <div class="float-xs-right">

            <form class="form-inline" action="/search" method="post">
                <div class="form-group">
                    <input type="text" class="form-control" id="searchText" name="searchText"
                           placeHolder="Text to search in all pages">
                </div>
                <button type="submit" class="btn btn-primary" id="searchButton">Search</button>
            </form>
        </div>


        <h1 class="display-4">${context.title}</h1>
        <div class="float-xs-right">
            <a class="btn btn-outline-danger" href="/logout" role="button" aria-pressed="true">Logout (${context.username})</a>
        </div>
    </div>

    <div class="col-md-12 mt-1">
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
        <div class="float-xs-left">

            <form class="form-inline" action="/backup" method="get">
                <button type="submit" class="btn btn-primary" id="backupButton">Backup</button>
            </form>
        </div>
    </div>
</div>

<#include "footer.ftl">
