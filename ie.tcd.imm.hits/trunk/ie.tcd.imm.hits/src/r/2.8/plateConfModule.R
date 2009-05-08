## The workhorse function for the 'Plate Configuration' module. This will create the
## image plot indicating the position of controls and samples on the plates (currently
## only based on the content of plate configuration file! No updates based on screen
## log file) and wrap the result in appropriate HTML code. 
writeHtml.plateConf <- function(cellHTSList, module, nrPlate, posControls,
                                negControls, con)
{
    outdir <- dirname(module@url)
    xr <- cellHTSList$raw
    if(state(xr)[["configured"]])
    {
        ## Create the image plots of the plate configuration as jpg and pdf
        res <- makePlot(outdir, con=con, name="configurationAsScreenPlot", w=7,
                        h=7*pdim(xr)["nrow"]/pdim(xr)["ncol"]*ceiling(nrPlate/6)/6+0.5,
                        psz=8,
                        fun=function()
                    {
                        do.call("configurationAsScreenPlot", 
                                args=list(x=xr, verbose=FALSE,
                                posControls=unlist(posControls),
                                negControls=negControls))
                    },
                        print=FALSE, isImageScreen=FALSE)
        ## Wrap as chtsImage object to get nice HTML layout
        img <- chtsImage(data.frame(thumbnail="configurationAsScreenPlot.png",
                                    fullImage="configurationAsScreenPlot.pdf",
                                    title="Plate Configuration"))
        ## A color legend for the plot
        mat <- matrix(NA, ncol=length(res), nrow=1)
        mat <- rbind(mat, names(res))
        img@additionalCode <- hwrite(mat, border=FALSE, bgcolor=rbind(res, NA),
                                     center=TRUE, table.class="plateConfModule legend",
                                     class="plateConfModule legend",
                                     style="width:30px; border: 1px solid #6699cc;")
        ## Now we produce the necessary HTML
        writeHtml.header(con)
        writeHtml(img, con=con)
        writeHtml.trailer(con)
        return(NULL)
    }
    else
    {
        return(NA)
    }
}
