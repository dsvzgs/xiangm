library(ggplot2)

working_dir <- "C:/xiangmu/Fog-Computing-multiDeviceDebug/src/mengxu"
setwd(working_dir)

sprintf("------------------------Start------------------------------")

result.df <- data.frame(jobID = integer(),
                        taskID = integer(),
                        processorType = character(),
                        processor = integer(),
                        startTime = double(),
                        completeTime = double()
)

testfile <- paste0("scheduling.csv")
result.df <- read.csv(paste0(testfile), header = TRUE)

# par(pin = c(10,5))
pdf('scheduling.pdf', width=18,height=6)
plot(1:5,1:5,xlim = c(-70,1180), ylim = c(-0.5,5.5), type = "n", cex.axis = 1.5, font.axis = 1, yaxt = "n", ann = FALSE)
par(mai=c(0,2.2,0,0)) #下，左，上，右留白
# plot("Time","Processor",xlim = c(0,950), ylim = c(-0.5,5.5), type = "n", cex.axis = 2, font.axis = 1)

# plot("time", "processor", xlim = c(0,950), ylim = c(-0.5,5.5), type = "n")


for(pross in 1:6){
  pross <- pross - 1
  rect(xleft = 0, ybottom = pross-0.3, xright = 1200, ytop = pross+0.3, lwd = 1, lty = 2)
}
color <- c("#FFD700", "#E3E3E3")

# color <- c("#ef5285", "#60c5ba")
# colorUpDown <- c("#a5dff9", "#feee7d")
c = color[1]

for(pos in 1:nrow(result.df)){
  row <- result.df[pos,]
  txt <- paste0(row$jobID, ".", row$taskID);

  if(row$jobID == 0){
    c = color[1]
  }
  else{
    c = color[2]
  }

  # rect(xleft = row$uploadTime, ybottom = row$processorID-0.3, xright = row$startTime, ytop = row$processorID+0.3, col = colorUpDown[1], lwd = 1)
  rect(xleft = row$startTime, ybottom = row$processorID-0.3, xright = row$completeTime, ytop = row$processorID+0.3, col = c, lwd = 1)
  # rect(xleft = row$completeTime, ybottom = row$processorID-0.3, xright = row$downloadTime, ytop = row$processorID+0.3, col = colorUpDown[2], lwd = 1)
  text((row$startTime+row$completeTime)/2, row$processorID+0.02, txt, cex = 0.8)
}

axis(side = 2, at = c(0, 1, 2, 3, 4, 5), labels = paste(c("Device 0","Device 1", "Edge 0", "Edge 1", "Cloud 0","Cloud 1")), las =2, cex.axis = 1.4, xpd = TRUE)
title(xlab= 'Time', cex.lab = 2)
# axis(side = 2, at = c(0, 1, 2, 3, 4, 5), labels = c("Device 0","Device 1", "Fog 0", "Fog 1", "Cloud 0","Cloud 1"))
# scale_y_continuous(limits=c(-0.5,5.5),breaks = c(0, 1, 2, 3, 4, 5),labels=c("Device 0", "Device 1", "Fog 0", "Fog 1", "Cloud 0","Cloud 1"))

