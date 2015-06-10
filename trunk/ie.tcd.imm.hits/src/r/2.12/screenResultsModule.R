## The Workhorse function for the 'Screen Results' module. This writes the topTable outout
## into a downloadable ASCII file and also produces some nice sortable HTML output.
writeHtml.screenResults <- function(cellHTSList, file="topTable.txt", verbose=interactive(),
                                    overallState, con, channels, colOrder, ...)
{
     if(overallState["scored"]){
		 out <- getTopTable(cellHTSList, file=file, verbose=verbose, channels=channels, colOrder=colOrder)
         keep <- grep("^plate$|^well$|^score|^wellAnno|^finalWellAnno|raw_|normalized_|GeneID|GeneSymbol",
                      colnames(out))
         #sel <- !(is.na(out$score))
         #out <- out[sel,keep]
         out <- out[,keep]
         writeHtml.header(con)
         writeLines(sprintf(paste("<div class=\"download\"%s><a href=\"%s\" target=\"_new\"><img",
                                  "src=\"textfileIcon.jpg\"><br>txt version</a></div>"),
                            addTooltip("downloadTable"),
                            file.path("..", "in", basename(file))), con)
         if(length(unlist(out)) > 20000)
         {
             writeLines("<div class=\"alert\">Result table too big to render.<br>
                         Please download txt version using the link to the left.</div>", con)
         }
         else
         {
             #out <- rbind(colnames(out), out)
             classes <- plateListClass(out, rep(1, nrow(out)))
             #classes[1,] <- NA
             hwrite(out, table.class="sortable", border=FALSE, center=TRUE, page=con, class=classes,
                    row.names=FALSE)
         }             
         writeHtml.trailer(con)
         return(NULL)
     }
     else
     {
         return(NA)
     }
 }
