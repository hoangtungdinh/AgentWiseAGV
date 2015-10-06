rm(list=ls())
library(Rmisc)
library(ggplot2)

pd <- position_dodge(1)

####################################

caBaselineMakeSpan = read.table("ResultsCABaseline_makespan.txt", header=TRUE)
caBaselineMakeSpan[,"approach"] = "ContextAwareBaseline"

caRepairMakeSpan = read.table("ResultsCARepair_makespan.txt", header=TRUE)
caRepairMakeSpan[,"approach"] = "ContextAwareRepair"

dMasMakeSpan = read.table("ResultsDMAS_makespan.txt", header=TRUE)
dMasMakeSpan[,"approach"] = "DelegateMAS"

makespan_dat = rbind(caBaselineMakeSpan, caRepairMakeSpan, dMasMakeSpan)

makespan_datC = summarySE(makespan_dat, measurevar="makespan", groupvars=c("numAGVs", "approach"))

ggplot(makespan_datC, aes(x=numAGVs, y=makespan, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=makespan-sd, ymax=makespan+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=makespan_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmakespan.pdf', width = 8, height = 4)

####################################

caBaselinePlanCost = read.table("ResultsCABaseline_plancost.txt", header=TRUE)
caBaselinePlanCost[,"approach"] = "ContextAwareBaseline"

caRepairPlanCost = read.table("ResultsCARepair_plancost.txt", header=TRUE)
caRepairPlanCost[,"approach"] = "ContextAwareRepair"

dMasPlanCost = read.table("ResultsDMAS_plancost.txt", header=TRUE)
dMasPlanCost[,"approach"] = "DelegateMAS"

planCost_dat = rbind(caBaselinePlanCost, caRepairPlanCost, dMasPlanCost)

planCost_datC <- summarySE(planCost_dat, measurevar="PlanCost", groupvars=c("numAGVs", "approach"))

ggplot(planCost_datC, aes(x=numAGVs, y=PlanCost, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=PlanCost-sd, ymax=PlanCost+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=planCost_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicplancost.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfMakeSpanCABaseline = c()
percentageOfMakeSpanCARepair = c()
percentageOfMakeSpanDMAS = c()

for (i in 1:100) {
  numAGVs[i] = caBaselineMakeSpan[i,1]
  percentageOfMakeSpanCABaseline[i] = 100
  percentageOfMakeSpanCARepair[i] = (caRepairMakeSpan[i,2] / caBaselineMakeSpan[i,2])*100
  percentageOfMakeSpanDMAS[i] = (dMasMakeSpan[i,2] / caBaselineMakeSpan[i,2])*100
}

percentageMSCABaseline = data.frame(numAGVs, percentageOfMakeSpanCABaseline)
colnames(percentageMSCABaseline)[2] = "percentage"
percentageMSCARepair = data.frame(numAGVs, percentageOfMakeSpanCARepair)
colnames(percentageMSCARepair)[2] = "percentage"
percentageMSDMAS = data.frame(numAGVs, percentageOfMakeSpanDMAS)
colnames(percentageMSDMAS)[2] = "percentage"

percentageMSCABaseline[,"approach"] = "ContextAwareBaseline"
percentageMSCARepair[,"approach"] = "ContextAwareRepair"
percentageMSDMAS[,"approach"] = "DMAS"

percentageMS = rbind(percentageMSCABaseline, percentageMSCARepair, percentageMSDMAS)

percentageMSC <- summarySE(percentageMS, measurevar="percentage", groupvars=c("numAGVs", "approach"))

ggplot(percentageMSC, aes(x=numAGVs, y=percentage, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=percentage-sd, ymax=percentage+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentageMSC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan DMAS / Makespan CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmakespanpercentage.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfPlanCostCABaseline = c()
percentageOfPlanCostCARepair = c()
percentageOfPlanCostDMAS = c()

for (i in 1:100) {
  numAGVs[i] = caBaselinePlanCost[i,1]
  percentageOfPlanCostCABaseline[i] = 100
  percentageOfPlanCostCARepair[i] = (caRepairPlanCost[i,2] / caBaselinePlanCost[i,2])*100
  percentageOfPlanCostDMAS[i] = (dMasPlanCost[i,2] / caBaselinePlanCost[i,2])*100
}

percentagePCCABaseline = data.frame(numAGVs, percentageOfPlanCostCABaseline)
colnames(percentagePCCABaseline)[2] = "percentage"
percentagePCCARepair = data.frame(numAGVs, percentageOfPlanCostCARepair)
colnames(percentagePCCARepair)[2] = "percentage"
percentagePCDMAS = data.frame(numAGVs, percentageOfPlanCostDMAS)
colnames(percentagePCDMAS)[2] = "percentage"

percentagePCCABaseline[,"approach"] = "ContextAwareBaseline"
percentagePCCARepair[,"approach"] = "ContextAwareRepair"
percentagePCDMAS[,"approach"] = "DMAS"

percentagePC = rbind(percentagePCCABaseline, percentagePCCARepair, percentagePCDMAS)

percentagePCC <- summarySE(percentagePC, measurevar="percentage", groupvars=c("numAGVs", "approach"))

ggplot(percentagePCC, aes(x=numAGVs, y=percentage, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=percentage-sd, ymax=percentage+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentagePCC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost DMAS / Plancost CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicplancostpercentage.pdf', width = 8, height = 4)

####################################

caMakeSpan = read.table("ResultsMultiCA_makespan.txt", header=TRUE)
caMakeSpan[,"approach"] = "ContextAwareBaseLine"

caRepairMakeSpan = read.table("ResultsMultiCArepair_makespan.txt", header=TRUE)
caRepairMakeSpan[,"approach"] = "ContextAwareRepair"

dMasMakeSpan = read.table("ResultsMultiDMAS_makespan.txt", header=TRUE)
dMasMakeSpan[,"approach"] = "DelegateMAS"

makespan_dat = rbind(caMakeSpan, dMasMakeSpan, caRepairMakeSpan)

makespan_datC = summarySE(makespan_dat, measurevar="makespan", groupvars=c("numAGVs", "approach"))

ggplot(makespan_datC, aes(x=numAGVs, y=makespan, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=makespan-sd, ymax=makespan+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=makespan_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultimakespan.pdf', width = 8, height = 4)

####################################

caPlanCost = read.table("ResultsMultiCA_plancost.txt", header=TRUE)
caPlanCost[,"approach"] = "ContextAwareBaseLine"

caRepairPlanCost = read.table("ResultsMultiCA_plancost.txt", header=TRUE)
caRepairPlanCost[,"approach"] = "ContextAwareRepair"

dMasPlanCost = read.table("ResultsMultiDMAS_plancost.txt", header=TRUE)
dMasPlanCost[,"approach"] = "DelegateMAS"

planCost_dat = rbind(caPlanCost, dMasPlanCost, caRepairPlanCost)

planCost_datC <- summarySE(planCost_dat, measurevar="PlanCost", groupvars=c("numAGVs", "approach"))

ggplot(planCost_datC, aes(x=numAGVs, y=PlanCost, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=PlanCost-sd, ymax=PlanCost+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=planCost_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultiplancost.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfMakeSpan = c()

for (i in 1:1000) {
  numAGVs[i] = caMakeSpan[i,1]
  percentageOfMakeSpan[i] = (dMasMakeSpan[i,2] / caMakeSpan[i,2])*100
}

percentageMS = data.frame(numAGVs, percentageOfMakeSpan)

percentageMSC <- summarySE(percentageMS, measurevar="percentageOfMakeSpan", groupvars=c("numAGVs"))

ggplot(percentageMSC, aes(x=numAGVs, y=percentageOfMakeSpan)) + 
  geom_errorbar(aes(ymin=percentageOfMakeSpan-sd, ymax=percentageOfMakeSpan+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentageMSC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan DMAS / Makespan CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultimakespanpercentage.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfPlanCost = c()

for (i in 1:1000) {
  numAGVs[i] = caMakeSpan[i,1]
  percentageOfPlanCost[i] = (dMasPlanCost[i,2] / caPlanCost[i,2])*100
}

percentageMS = data.frame(numAGVs, percentageOfPlanCost)

percentageMSC <- summarySE(percentageMS, measurevar="percentageOfPlanCost", groupvars=c("numAGVs"))

ggplot(percentageMSC, aes(x=numAGVs, y=percentageOfPlanCost)) + 
  geom_errorbar(aes(ymin=percentageOfPlanCost-sd, ymax=percentageOfPlanCost+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentageMSC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost DMAS / Plancost CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultiplancostpercentage.pdf', width = 8, height = 4)

# #######################################
# caMultiStage = read.table("ResultsMultiCA.txt", header=TRUE)
# caMultiStage[,"approach"] = "ContextAware"
# 
# dMasMultiStage = read.table("ResultsMultiDMAS.txt", header=TRUE)
# dMasMultiStage[,"approach"] = "DelegateMAS"
# 
# throughput_dat = rbind(caMultiStage, dMasMultiStage)
# 
# throughput_datC = summarySE(throughput_dat, measurevar="FinishedTask", groupvars=c("numAGVs", "approach"))
# 
# ggplot(throughput_datC, aes(x=numAGVs, y=FinishedTask, color=approach, group=approach)) + 
#   geom_errorbar(aes(ymin=FinishedTask-sd, ymax=FinishedTask+sd), width=3, position=pd) +
#   geom_line(position=pd) +
#   geom_point(size=3, position=pd) +
#   theme_classic() + 
#   scale_x_discrete(limits=throughput_datC[,1]) +
#   theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
#   coord_cartesian(xlim = c(5, 105)) +
#   xlab('Number of AGVs') +
#   ylab('Throughput') + 
#   theme(legend.title=element_blank()) +
#   theme(legend.justification=c(0,1), legend.position=c(0, 1))
# 
# ggsave('dynamicthrougput.pdf', width = 8, height = 4)
# 
# ################################################
# 
# numAGVs = c()
# percentageOfThroughPut = c()
# 
# for (i in 1:nrow(caMultiStage)) {
#   numAGVs[i] = caMultiStage[i,1]
#   percentageOfThroughPut[i] = (dMasMultiStage[i,2] / caMultiStage[i,2])*100
# }
# 
# percentageThroughPut = data.frame(numAGVs, percentageOfThroughPut)
# 
# percentageThroughPutC <- summarySE(percentageThroughPut, measurevar="percentageOfThroughPut", groupvars=c("numAGVs"))
# 
# ggplot(percentageThroughPutC, aes(x=numAGVs, y=percentageOfThroughPut)) + 
#   geom_errorbar(aes(ymin=percentageOfThroughPut-sd, ymax=percentageOfThroughPut+sd), width=3, position=pd) +
#   geom_line(position=pd) +
#   geom_point(size=3, position=pd) +
#   theme_classic() + 
#   scale_x_discrete(limits=percentageMSC[,1]) +
#   theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
#   coord_cartesian(xlim = c(5, 105)) +
#   xlab('Number of AGVs') +
#   ylab('Throughput DMAS / Throughput CA') + 
#   theme(legend.title=element_blank()) +
#   theme(legend.justification=c(0,1), legend.position=c(0, 1))
# 
# ggsave('dynamicthrougputpercentage.pdf', width = 8, height = 4)