## The workhorse function for the 'Screen Description module': a separate page with the info
## from the description file
writeHtml.screenDescription <- function(cellHTSList, module, overallState, outFile, con, ...)
{
    if(overallState["configured"])
    {
        xr <- cellHTSList$raw
        writeLines(screenDesc(xr), outFile)
        script <- paste(screenDesc(xr), collapse="<br>")
        writeHtml.header(con)
        method <- getProInfo(cellHTSList)
        script <- c(paste(names(method), unlist(method), sep=": ", collapse="<br>"),
                    "<br><br>", script)
        writeLines(c("<p class=\"verbatim\">", script, "</p>"), con)
        writeHtml.trailer(con)
        return(NULL)
    }
    else
    {
        return(NA)
    }
}



getProInfo <- function(cellHTSlist)
{
    n <- c("normalized", "summarized", "scored")
    info <- cellHTSlist[[length(cellHTSlist)]]@processingInfo[n]
    sel <- !sapply(info, is.null)
    return(info[sel]) 
}

