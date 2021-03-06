/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package eu.supersede.dm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.supersede.fe.exception.NotFoundException;
import eu.supersede.gr.model.HActivity;
import eu.supersede.gr.model.HAlert;
import eu.supersede.gr.model.HProcess;
import eu.supersede.gr.model.HProcessCriterion;
import eu.supersede.gr.model.HProcessMember;
import eu.supersede.gr.model.HProperty;
import eu.supersede.gr.model.HPropertyBag;
import eu.supersede.gr.model.HRequirementScore;
import eu.supersede.gr.model.HRequirementsRanking;
import eu.supersede.gr.model.ProcessStatus;
import eu.supersede.gr.model.Requirement;
import eu.supersede.gr.model.RequirementStatus;
import eu.supersede.gr.model.ValutationCriteria;

public class PersistedProcess extends AbstractProcessManager
{
    private Long processId;

    public PersistedProcess(Long processId)
    {
        this.processId = processId;
    }

    @Override
    public void addRequirement(Requirement r)
    {
        r.setProcessId(processId);
        DMGame.get().jpa.requirements.save(r);
    }

    @Override
    public List<Requirement> getRequirements()
    {
        return DMGame.get().jpa.requirements.findRequirementsByProcessId(processId);
    }

    @Override
    public int getRequirementsCount()
    {
        return getRequirements().size();
    }

    @Override
    public void setRequirementsStatus(List<Requirement> reqs, Integer status)
    {
        for (Requirement r : reqs)
        {
            if (isValidNextState(r.getStatus(), status))
            {
                r.setStatus(status);
                DMGame.get().jpa.requirements.save(r);
            }
        }
    }

    @Override
    public Long addProcessMember(Long userId, String role)
    {
        HProcessMember m = new HProcessMember();
        m.setProcessId(processId);
        m.setUserId(userId);
        m.setRole(role);
        m = DMGame.get().jpa.members.save(m);
        return m.getId();
    }

    @Override
    public List<HProcessMember> getProcessMembers()
    {
        return DMGame.get().jpa.members.findProcessMembers(processId);
    }

    @Override
    public List<HProcessMember> getProcessMembers(String role)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HActivity createActivity(String methodName, Long userId)
    {
        HActivity a = new HActivity();
        a.setProcessId(processId);
        a.setUserId(userId);
        a.setMethodName(methodName);
        return DMGame.get().jpa.activities.save(a);
    }

    @Override
    public void addAlert(HAlert alert)
    {
        alert.setProcessId(processId);
        DMGame.get().jpa.alerts.save(alert);
    }

    @Override
    public List<HAlert> getAlerts()
    {
        return DMGame.get().jpa.alerts.findAlertsByProcess(processId);
    }

    @Override
    public void removeAlert(String id)
    {
        HAlert alert = DMGame.get().jpa.alerts.findOne(id);

        if (alert == null)
        {
            throw new NotFoundException("Can't remove alert with id " + id + " because it does not exist");
        }

        alert.setProcessId(-1L);
        DMGame.get().jpa.alerts.save(alert);
    }

    @Override
    public List<HActivity> getOngoingActivities()
    {
        return DMGame.get().jpa.activities.findByProcessId(processId);
    }

    @Override
    public void addCriterion(ValutationCriteria vc)
    {
        HProcessCriterion c = new HProcessCriterion();
        c.setSourceId(vc.getCriteriaId());
        c.setDescription(vc.getDescription());
        c.setProcessId(this.processId);
        c.setName(vc.getName());
        DMGame.get().jpa.processCriteria.save(c);
    }

    @Override
    public List<ValutationCriteria> getCriteria()
    {
        List<ValutationCriteria> list = new ArrayList<>();
        List<HProcessCriterion> procList = DMGame.get().jpa.processCriteria.findByProcessId(this.processId);

        for (HProcessCriterion pc : procList)
        {
            ValutationCriteria v = new ValutationCriteria();
            v.setCriteriaId(pc.getSourceId());
            v.setDescription(pc.getDescription());
            v.setName(pc.getName());
            v.setUserCriteriaPoints(new ArrayList<>());
            list.add(v);
        }

        return list;
    }

    @Override
    public List<HProcessCriterion> getProcessCriteria()
    {
        return DMGame.get().jpa.processCriteria.findByProcessId(this.processId);
    }

    @Override
    public int getCriteriaCount()
    {
        return DMGame.get().jpa.processCriteria.findByProcessId(this.processId).size();
    }

    @Override
    public List<HActivity> getOngoingActivities(String methodName)
    {
        return DMGame.get().jpa.activities.findByProcessAndMethodName(processId, methodName);
    }

    @Override
    public PropertyBag getProperties(HActivity a)
    {
        HPropertyBag bag;

        if (a.getPropertyBag() != null)
        {
            bag = DMGame.get().jpa.propertyBags.findOne(a.getPropertyBag());
        }
        else
        {
            bag = new HPropertyBag();
            bag = DMGame.get().jpa.propertyBags.save(bag);
        }

        a.setPropertyBag(bag.getId());
        a = DMGame.get().jpa.activities.save(a);

        return new PropertyBag(a);
    }

    @Override
    public Requirement getRequirement(Long reqId)
    {
        return DMGame.get().getJpa().requirements.findOne(reqId);
    }

    @Override
    public void removeRequirement(Long reqId)
    {
        Requirement r = getRequirement(reqId);

        if (r != null)
        {
            r.setProcessId(-1L);

            // Reset the requirement status only if it is not enacted
            if (!r.getStatus().equals(RequirementStatus.Enacted.getValue()))
            {
                r.setStatus(RequirementStatus.Unconfirmed.getValue());
            }

            DMGame.get().jpa.requirements.save(r);
        }
    }

    @Override
    public void removeCriterion(Long criterionId, Long sourceId, Long processId)
    {
        DMGame.get().getJpa().processCriteria.deleteById(criterionId, sourceId, processId);
    }

    @Override
    public void removeProcessMember(Long id, Long userId, Long processId)
    {
        DMGame.get().getJpa().members.deleteById(id, userId, processId);
    }

    @Override
    public ProcessStatus getProcessStatus()
    {
        return getProcess().getStatus();
    }

    @Override
    public HProcess getProcess()
    {
        HProcess process = DMGame.get().getJpa().processes.findOne(processId);

        if (process == null)
        {
            throw new NotFoundException("Process with id " + processId + " does not exist");
        }

        return process;
    }

    @Override
    public void setProcessStatus(ProcessStatus status)
    {
        HProcess process = getProcess();

        if (process == null)
        {
            throw new NotFoundException(
                    "Can't change status of process with id " + processId + " because it does not exist");
        }

        process.setStatus(status);
        DMGame.get().getJpa().processes.save(process);
    }

    @Override
    public void deleteActivity(HActivity a)
    {
        PropertyBag bag = getProperties(a);
        List<HProperty> properties = bag.getProperties();

        for (HProperty p : properties)
        {
            DMGame.get().getJpa().properties.delete(p.getId());
        }

        DMGame.get().getJpa().propertyBags.delete(bag.id);
        DMGame.get().getJpa().activities.delete(a.getId());
    }

    @Override
    public String getCurrentPhase()
    {
        HProcess process = getProcess();
        String phaseName = getProcessPhaseName(process);

        if (phaseName == null)
        {
            phaseName = DMGame.get().getLifecycle().getInitPhase().getName();
        }

        return phaseName;
    }

    @Override
    public Collection<String> getNextPhases()
    {
        DMLifecycle lifecycle = DMGame.get().getLifecycle();
        DMPhase phase = lifecycle.getPhase(getCurrentPhase());
        List<String> phases = new ArrayList<>();

        for (DMPhase n : phase.getNextPhases())
        {
            phases.add(n.getName());
        }

        return phases;
    }

    public String getProcessPhaseName(HProcess process)
    {
        String phaseName = process.getPhaseName();

        if (phaseName == null)
        {
            phaseName = DMGame.get().getLifecycle().getInitPhase().getName();
        }

        return phaseName;
    }

    @Override
    public void setNextPhase(String phaseName) throws Exception
    {
        try
        {
            HProcess process = getProcess();
            DMGame.get().getLifecycle().getPhase(getProcessPhaseName(process)).checkPreconditions(this);
            process.setPhaseName(phaseName);
            DMGame.get().getJpa().processes.save(process);
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    @Override
    public List<RequirementsRanking> getRankings()
    {
        List<HRequirementsRanking> hlist = DMGame.get().getJpa().requirementsRankings
                .findRankingsByProcessId(this.processId);
        List<RequirementsRanking> list = new ArrayList<>();

        for (HRequirementsRanking requirementsRanking : hlist)
        {
            RequirementsRanking ranking = new RequirementsRanking();
            ranking.setProcessId(requirementsRanking.getProcessId());
            ranking.setName(requirementsRanking.getName());
            ranking.setSelected(requirementsRanking.isSelected());
            List<HRequirementScore> scores = DMGame.get().getJpa().scoresJpa
                    .findByProcessIdAndRankingName(requirementsRanking.getProcessId(), requirementsRanking.getName());
            ranking.setScores(scores);
            list.add(ranking);
        }

        return list;
    }

    @Override
    public HRequirementsRanking createRanking(String name)
    {
        HRequirementsRanking rr = new HRequirementsRanking();
        rr.setProcessId(processId);
        rr.setName(name);
        return DMGame.get().getJpa().requirementsRankings.save(rr);
    }

    @Override
    public RequirementsRanking getRankingByName(String name)
    {
        HRequirementsRanking requirementsRanking = DMGame.get().getJpa().requirementsRankings
                .findRankingsByProcessIdAndName(processId, name);

        if (requirementsRanking == null)
        {
            return null;
        }

        RequirementsRanking ranking = new RequirementsRanking();
        ranking.setProcessId(requirementsRanking.getProcessId());
        ranking.setName(requirementsRanking.getName());
        ranking.setSelected(requirementsRanking.isSelected());
        ranking.setEnacted(requirementsRanking.isEnacted());
        ranking.setScores(DMGame.get().getJpa().scoresJpa
                .findByProcessIdAndRankingName(requirementsRanking.getProcessId(), requirementsRanking.getName()));

        return ranking;
    }
}