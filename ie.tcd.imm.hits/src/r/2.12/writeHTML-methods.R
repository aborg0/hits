## Set up cellHTS HTML pages including all necessary javascript and css links
writeHtml.header <- function(con, path=".", title="cellHTS2 Experiment Report")
{
    doc <- "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\""
    out <-sprintf("%s
<html>
  <head>
    <title>
      %s
    </title>
    <link rel=\"stylesheet\" href=\"%s/cellHTS.css\" type=\"text/css\">
    <script type=\"text/javascript\" src=\"%s/cellHTS.js\"></script>
    <script src=\"%s/sorttable.js\"></script>
   </head>
   <body onload=\"initialize();\">
     <script type=\"text/javascript\" src=\"%s/wz_tooltip.js\"></script>", doc[1],
                  title, path, path, path, path)
    if(!missing(con))
        writeLines(out, con)
    return(invisible(out))
}



## Closing HTML code
writeHtml.trailer <- function(con)
{
    out <-
"  </body>
</html>"
    if(!missing(con))
        writeLines(out, con)
    return(invisible(out)) 
}

 


## write HTML output for a single tab. The following mandatory arguments have to be set:
##   title: text used as a title for the tab
##   id: a character scalar used as identifier for the tab collection in javascript
##       (this identifies the collection of tabs, there may be multiple collections on one page)
##   script: a character containing the java script that is associated to the tab
##   url: url to another HTML page which is supposed to be opened in the iFrame as result
##        of clicking the tab
##   size: there are three different sizes of tabs: big, medium and small
##   con: a connection object
##   class: the css class of the tag, one in 'selected' or 'unselected'
writeHtml.tab <- function(title, url, id="mainTab",
                          script=sprintf("toggleTabById('%s', this, '%s')", id, url),
                          class, size, con)
{
    out <- sprintf("
	    <table class=\"%s %s %s\" onClick=\"%s\">
	      <tr>
		<td class=\"left\">
		  &nbsp&nbsp
		</td>
		<td class=\"middle\">
                  <span>
		     %s
                  </span>
		</td>
		<td class=\"right\">
		  &nbsp&nbsp		
		</td>
	      </tr>
	    </table>" , size, class, id, script, title)
    if(!missing(con))
        writeLines(out, con)
    return(invisible(out)) 
}



## Produce HTML output for a whole collection of tabs.
##   tabs: a data frame with all necessary values for the tabs. Each row will be supplied as
##         argument list to 'writeHtml.tab' via 'do.call'. The con, class, id, script and size
##         arguments don't have to be supplied again, they are taken from the function arguments
##         or are initialized as needed.
##   con: a connection object
writeHtml.tabCollection <- function(tabs, size=c("big", "medium", "small"), con)
{
    size <- match.arg(size)
    tabs$size <- size
    out <- sprintf("
    <div class=\"tab %s\">
      <table class=\"bar %s\">
	<tr>
	  <td class=\"topbar\">
	  </td>
	</tr>
	<tr>
	  <td class=\"bar\">
	  </td>
	</tr>
      </table>
      <table class=\"tabs\">
	<tr>
	  <td>", size, size)
    if(is.null(tabs$class))
        tabs$class <- c("selected", rep("unselected", nrow(tabs)-1))
    for(i in seq_len(nrow(tabs))){
        alist <- as.list(tabs[i,])
        alist$con <- NULL
        out <- c(out, do.call("writeHtml.tab", args=alist))
    }
    out <- c(out,"
           </td>
	</tr>
      </table>
    </div>")
    if(!missing(con))
        writeLines(out, con)
    return(invisible(out)) 
}



## write the overall HTML framework for a cellHTS report. This is mainly a bounding table, a set
## of tabs linking to further sub-pages and an iFrame which serves as canvas for these sub-pages.
## The following mandatory arguments have to be set
##   title: the experiment title
##   tabs: a data frame with all necessary values for the tabs. Each row will be supplied as
##         argument list to 'writeHtml.tab' via 'do.call'. The con, class, id, script and size
##         arguments don't have to be supplied again, they are taken from the function arguments
##         or are initialized as needed.
##   con: a connection object
writeHtml.mainpage <- function(title, tabs, con)
{
    writeHtml.header(con, path="html")
    writeLines(sprintf("
    <table class=\"border\">
      <tr class=\"border top\">
        <td class=\"border corner\">
          &nbsp&nbsp&nbsp&nbsp
          <div class=\"helpSwitch\">
	     help: 
	    <span onClick=\"toggleHelp(this);\" id=\"helpSwitch\"%s>
	    </span>
	  </div>
        </td>
        <td class=\"border top\">
          <div class=\"header\">
	    Report for Experiment <span class=\"header\">%s</span>
          </div>
	  <div class=\"timestamp\">
            generated %s
          </div>
          <div class=\"logo\">
	  </div>
        </td>
      </tr>
      <tr class=\"border middle\">
        <td class=\"border left\"></td>
        <td class=\"main\">", addTooltip("switchHelp"),
                       title, paste(format(Sys.time(), "%a %b %d %H:%M %Y"), "   (<small>version ",
                                    package.version("cellHTS2"), "</small>)", sep="")), con)
    tabs <- tabs[!apply(tabs, 1, function(y) all(is.na(y))),]
    writeHtml.tabCollection(tabs, size="medium", con=con)
    writeLines(sprintf("
          <div class=\"main\">
	    <iframe class=\"main\" src=\"%s\" name=\"main\" frameborder=\"0\" noresize id=\"main\"
              scrolling=\"auto\" marginwidth=\"0\" marginheight=\"0\"
              onload=\"if (window.parent && window.parent.autoIframe) {window.parent.autoIframe('main');}\">
	      <p>
	        Your browser does not support iFrames. This page will not work for you.
	      </p>
	    </iframe>
          </div>
        </td>
      </tr>
    </table>
  </body>
</html>", tabs[1,"url"]), con)
return(invisible(NULL))
}



## Create quasi-random guids. This is only based on the time stamp,
## not on MAC address or similar.
guid <- function()
    as.vector(format.hexmode(as.integer(Sys.time())/
                             runif(1)*proc.time()["elapsed"]))



## Call 'htmlFun' with 'funArgs' in a chtsModule object and generate all necessary HTML code. Setting the
## 'con' argument to NULL results in not opening a file connection. This might be handy in case one wants
## to link to an already existing file, or to handle the file generation directly in the HTML function.
setMethod("writeHtml",
          signature=signature("chtsModule"),
          definition=function(x, con, cellHTSList)
      {
          if(missing(con))
          {
              if(!file.exists(dirname(x@url)))
                  dir.create(dirname(x@url), recursive=TRUE, showWarnings=FALSE)
              con <- file(x@url, open="w")
          }
          if(!is.null(con))
              on.exit(close(con))
          alist <- x@funArgs
          if(! "cellHTSList" %in% names(alist))
          {
              if(missing(cellHTSList))
                  stop("Argument 'cellHTSList' has to be supplied, either as part of the argument ",
                       "list or as a separate parameter.")
              alist$cellHTSList <- cellHTSList
              if(!all(is(alist@cellHTSList, "cellHTS")))
                  stop("The 'cellHTSList' has to be a list of cellHTS objects.")
          }
          alist$module <- x
          alist$con <- con
          tmp <- do.call(x@htmlFun, args=alist)
          if(!is.null(tmp) && is.na(tmp))
              return(NA)
          url <- file.path("html", basename(x@url))
          title <- if(!length(x@title)) NA else x@title
          res <- data.frame(title=title, url=url,
                            script=sprintf("toggleTabById('%s', this, '%s');\"%s",
                            "mainTab", url, addTooltip(title, trailer="")))
          if(!is.null(tmp) && is.list(tmp) && names(tmp) %in% c("id", "total", "class"))
              res <- cbind(res, tmp)
          return(invisible(res))
      })



## coerce chtsImage to data.frame
setAs(from="chtsImage", to="data.frame", def=function(from)
  {
      ltm <- max(sapply(slotNames(from), function(x) length(slot(from, x))))
      tm <- if(!length(from@thumbnail)) rep(NA, ltm)  else from@thumbnail
      ltm <- length(tm)
      st <- if(!length(from@shortTitle)) "foo" else from@shortTitle
      ti <- if(!length(from@title)) rep(NA, ltm)  else from@title
      ca <- if(!length(from@caption)) rep(NA, ltm) else from@caption
      fi <- if(!length(from@fullImage)) rep(NA, ltm) else from@fullImage
      map <- if(!length(from@map)) rep(NA, ltm) else from@map
      ac <- if(!length(from@additionalCode)) rep(NA, ltm) else from@additionalCode
      tt <- if(!length(from@tooltips)) rep(NA, ltm) else from@tooltips
      df <- suppressWarnings(data.frame(ID=seq_len(ltm), Title=I(ti), Caption=I(ca), FullImage=I(fi),
                                        Pdf=I(sapply(fi, function(y) ifelse(is.na(y), "", "pdf"))),
                                        Thumbnail=I(tm), Class=from@jsclass, AdditionalCode=ac, Map=I(map),
                                        row.names=NULL, stringsAsFactors=FALSE))
      if(any(is.na(df)))
           df[is.na(df)] <- I("")
      return(df)
  })



## Create HTML for chtsImage objects. If there are multiple images
## in the object they will be stacked (if vertical==TRUE) or aligned horizontally.
## If there is a link to a pdf version, this will also be created.
setMethod("writeHtml",
          signature=signature("chtsImage"),
          definition=function(x, con, vertical=TRUE)
      {
          st <- x@shortTitle
          if(!length(st))
              st <- "foo"
          tabs <- data.frame(title=st, id=seq_along(st))
          imgs <- as(x, "data.frame")
          out <- if(length(st)>1 && !vertical) "<table align=\"center\" class=\"horImg\">\n<tr>\n<td>" else "" 
          for(i in seq_len(nrow(tabs))){
              out <- c(out, sprintf("
                <table class=\"image %s\" align=\"center\">
                  <tr>
                    <td class=\"header\">
                      <div class=\"header\">
	                %s
                      </div>
                      <div class=\"caption\">
	                %s
                      </div>
                    </td>
                  </tr>",  
                                    unique(imgs[i,"Class"]),
                                    imgs[i, "Title"], imgs[i, "Caption"]))
              if(imgs[i, "Thumbnail"] != "")
                  out <- c(out, sprintf("
               <tr>
                    <td class=\"main\">
                      <img class=\"image\" src=\"%s\" %s>
                        %s
                     </td>
                  </tr>
                  <tr>
                    <td class=\"pdf\">
                      <span class=\"pdf\" onClick=\"linkToPdf('%s');\"%s>
                        %s
                      </span>
                    </td>
                  </tr>",
                                        imgs[i, "Thumbnail"],
                                        imgs[i, "Map"], imgs[i, "AdditionalCode"], imgs[i, "FullImage"],
                                        addTooltip("pdf"), imgs[i, "Pdf"]))
              out <- c(out, "</table>")
              if(length(st)>1 && i < nrow(tabs) && !vertical)
                  out <- c(out, "</td>\n<td>")
          } ## for(i...
          if(length(st)>1 && !vertical)
              out <- c(out, "</td>\n</tr>\n</table>")
          if(!missing(con))
              writeLines(out, con)
          return(invisible(out))
      })



## Create HTML output. The organisation of the image stack is as follows:
##   each channel will be on a separate tab
##   each replicates will be on a separate tab
##   individual images in the chtsImage object are horizontally aligned
setMethod("writeHtml",
          signature=signature("chtsImageStack"),
          definition=function(x, con, vertical=TRUE)
      {
          nrChan <- length(x@stack)
          nrRep <- unique(sapply(x@stack, length))
          class <- "imageStack"
          out <- sprintf("
            <table class=\"%s\" align=\"center\">
              %s
              <tr>
                <td class=\"tabs\">", class,
                         if(length(x@title)) sprintf("<tr><td class=\"header\">%s</td></tr>", x@title) else "")
          if(nrChan>1){
            channelNames = names(x@stack)
            if (is.null(channelNames)) channelNames = paste("Channel", seq_len(nrChan))
            chanTabs <- data.frame(title=channelNames,
                                     id=paste(x@id, "Channel", sep=""),
                                     script=sprintf("toggleTabByChannel('%sChannel', this, %d)", x@id,
                                     seq_len(nrChan)))
              out <- c(out, writeHtml.tabCollection(chanTabs, size="medium"))
          }
          imgs <- !is.null(names(x@stack[[1]]))
          title <- if(!imgs) paste("Replicate", seq_len(nrRep)) else names(x@stack[[1]])
          if(nrRep>1){
              tt <- if(length(x@tooltips)) paste("\"", x@tooltips) else ""
              repTabs <- data.frame(title=title,
                                    id=paste(x@id, "Replicate", sep=""),
                                    script=sprintf("toggleTabByReplicate('%sReplicate', this, %d) %s", x@id,
                                    seq_len(nrRep), tt))
              out <- c(out, writeHtml.tabCollection(repTabs, size="small"))
          }
          out <- c(out, "
              </td>
            </tr>
            <tr>
              <td>")
          for(i in seq_len(nrChan)){
              for(j in seq_len(nrRep)){
                  viz <- if(i==1 && j==1) "visible" else "invisible"             
                  img <- x@stack[[i]][[j]]
                  img@jsclass <- sprintf(" %s %s channel%d replicate%d", viz, x@id, i, j)
                  out <- c(out, writeHtml(img, vertical=vertical))
              }
          }
          out <- c(out, "
              </tr>
            </td>  
          </table>")
          if(!missing(con))
              writeLines(out, con)
          return(invisible(out))     
      })
