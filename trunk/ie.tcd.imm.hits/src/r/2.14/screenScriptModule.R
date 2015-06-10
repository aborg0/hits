## The workhorse function for the 'Screen Script module': a listing of the commands in the
## script generating this report, both as an HTML document and as a text file.
writeHtml.screenScript <- function(cellHTSList, module, mainScriptFile, outputFile, con, ...)
{
    if(is.na(mainScriptFile))
    {
        warning("The R script which produced this cellHTS2 report has not been provided ",
                "via the 'mainScriptFile' argument.\nWe recommend storing this ",
                "script for future reference along with the report.", call.=FALSE)
        return(NA)
    }
    else
    {
        if(!file.exists(mainScriptFile))
        {
            warning(sprintf("The provided R script '%s' does not exists and will not be added to the report.",
                            mainScriptFile))
            return(NA)
        }
        file.copy(from=mainScriptFile, to=outputFile, overwrite=TRUE)
    }
    script <- paste(readLines(mainScriptFile), collapse="<br>")
    writeHtml.header(con)
    writeLines(c("<p class=\"verbatim\">", script, "</p>"), con)
    writeHtml.trailer(con)
    return(invisible(NULL))
}
