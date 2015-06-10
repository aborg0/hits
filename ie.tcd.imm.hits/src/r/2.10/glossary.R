## A glossary is a 4 colums data.frame 
## col[1] : Words or terms to be defined
## col[2] : Simple definition of the word: these definitions are visible in
##          the html files on mouse-over.
## col[3] : A more complete definition of the word in column 1. These definitions
##          will be writen in glossary.html with the saveHtmlGlossary function.
##          If this is set to "", the term will not appear in the glossary and
##          only a regular, non-clickable tooltip will be generated.
## col[4] : A logical vector indicating wheter col[3]=="". This simply makes everything
##          a bit cleaner. As of Version 2.9.23 the terms are stored in an XML file in
##          inst/glossary/glossary.xml, which makes maintaining glossary much cleaner.
##          This file is parsed using the new 'parseGlossaryXML' which will create the
##          necessary data.frame.




## Saving the glossary as an html file.
saveHtmlGlossary <- function(glossary=parseGlossaryXML(), targetGlossary)
{  
    targetGlossaryFile <- con <- file(targetGlossary, open="w")
    on.exit(close(targetGlossaryFile))
    writeHtml.header(targetGlossaryFile, title="Glossary")
    hwrite('Glossary', targetGlossaryFile, heading=1, center=T, br=T)
    ## we only want the complete definitions in the html report
    htmlGlossary <- data.frame(word=glossary$word, def=glossary$completeDefinition)
    htmlGlossary <- htmlGlossary[htmlGlossary$def!="",]
    classes <- plateListClass(htmlGlossary)
    classes[,1] <- paste(classes[,1], "term")
    hwrite(htmlGlossary, targetGlossaryFile, col.names=FALSE, row.names=FALSE,
           class=classes, border=0, center=TRUE, col.width=c("25%", "75%"))
    writeHtml.trailer(targetGlossaryFile)
}


## Create tooltips for a vector of terms by fetching the respective
## definition from the glossary if fromGlossary==TRUE, alternative use
## the value of 'word' itself. 'link' controls whether a link to the
## glossary table should be included, and title will add a title
## banner to the tooltip. These two arguments are typically
## automatically set, depending on the value of 'word'. A precomputed
## glossary can be passed on to the function to avoid parsing the XML
## file over and over again. 'fullTag' controls whether to create a
## complete <span> tag or just the event handlers.
## 'fuzzy' enables fuzzing matching of the term to the glossary. 
addTooltip <- function(word, title, fromGlossary=TRUE, link, trailer="\"",
                       glossary=parseGlossaryXML(), fullTag=FALSE,
                       fuzzy=FALSE)
{
    res <- NULL
    for(i in word)
    {
        present <- if(fuzzy) length(grep(i, glossary$word, fixed=TRUE)) else (i %in% glossary$word)
        if(present || !fromGlossary)
        {
            desc <- if(fromGlossary) getDefinition(i, glossary, fuzzy) else
            structure(i, isDefinition=FALSE)
            isDef <- attr(desc, "isDefinition")
            linkTxt <- if((missing(link) && isDef) || (!missing(link) && link))
                "\" onClick=\"if(tt_Enabled) linkToFile('glossary.html');" else ""
            titleTxt <- if(missing(title)) if(isDef) "Definition" else "" else title
            tmp <- sprintf(paste(" onmouseover=\"Tip('%s', WIDTH, 250, TITLE, '%s',",
                                 "OFFSETX, 1);\" onmouseout=\"UnTip();%s%s"),
                           desc, titleTxt, linkTxt, trailer)
            if(fullTag)
                tmp <- sprintf("<span%s%s>%s</span>", if(isDef) " class=\"pointer\"" else"",
                               tmp, attr(desc, "fullTerm"))
            res <- c(res, tmp)
        }
        else
        {
            res <- c(res, i)
        }
    }
    return(res)
}
                               

## A very rudimentary XML parser. We don't want to depend on the XML
## package for this trivial procedure.
parseGlossaryXML <- function()
{
    xfile <- system.file(file.path("glossary", "glossary.xml"),
                         package="cellHTS2")
    xml <- readLines(xfile)
    xml <- gsub("^ *| *$|<!--.*-->", "", xml)
    xml <- gsub("\t", "", xml)
    xml <- xml[xml!=""]  
    msg <- sprintf("Malformed XML in file '%s'", xfile)
    if(xml[1] != "<glossary>" || tail(xml,1) != "</glossary>")
        stop(msg)
    terms <- extractTag(xml, "term", msg)
    word <- sapply(terms, function(x)
                    paste(unlist(extractTag(x, "word", msg)), collapse=" "))
    simpleDef <- sapply(terms, function(x)
                         paste(unlist(extractTag(x, "simpleDef", msg)), collapse=" "))
    completeDef <-  sapply(terms, function(x)
                            paste(unlist(extractTag(x, "completeDef", msg)),
                                  collapse=" "))
    if(length(unique(c(length(simpleDef), length(completeDef), length(word)))) !=1)
        stop(msg)
    return(data.frame(word, simpleDefinition=simpleDef, completeDefinition=completeDef,
                      isDefinition=completeDef != "", stringsAsFactors=FALSE))

}

## A little helper to find and extract the content of a particular tag
## type.
extractTag <- function(xml, tag, msg)
{
    beg <- grep(sprintf("<%s>", tag), xml)
    end <- grep(sprintf("</%s>", tag), xml)
    if(!length(beg) && !length(end))
        return(NULL)
    if((length(beg) != length(end)) || !all(beg<end))
        stop(msg)
    return(mapply(function(b,e) xml[(b+1):(e-1)], beg, end,
                  SIMPLIFY=FALSE))
}


## setDefinition enables to add a new definition into the glossary
## word : word to be defined
setDefinition <- function(glossary, word, simpleDefinition, completeDefinition="")
{
    toAdd <- data.frame(word=word, simpleDefinition=simpleDefinition,
                        completeDefinition=completeDefinition,
                        isDefinition=completeDefinition=="",
                        stringsAsFactors=FALSE)
    newGlossary <- rbind(glossary,toAdd)
    return(newGlossary)
}


## getDefinition returns the (simple) definition associated to the
## word given in argument
getDefinition <- function(word, glossary=parseGlossaryXML(), fuzzy=FALSE)
{
    index <- if(!fuzzy) match(word, glossary[,"word"]) else
    grep(word, glossary[,"word"], fixed=TRUE)[1]
    def <- glossary[index, "simpleDefinition"]
    attr(def, "isDefinition") <- glossary[index, "isDefinition"]
    attr(def, "fullTerm") <- glossary[index, "word"]
    return(def)	
}
