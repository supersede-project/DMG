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

package eu.supersede.dm.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.supersede.dm.DMGame;
import eu.supersede.dm.DMPhase;
import eu.supersede.dm.ProcessManager;
import eu.supersede.dm.methods.AccessRequirementsEditingSession;
import eu.supersede.gr.jpa.RequirementsDependenciesJpa;
import eu.supersede.gr.jpa.RequirementsPropertiesJpa;
import eu.supersede.gr.model.HActivity;
import eu.supersede.gr.model.HProcessMember;
import eu.supersede.gr.model.HRequirementDependency;
import eu.supersede.gr.model.HRequirementProperty;
import eu.supersede.gr.model.Requirement;
import eu.supersede.gr.model.RequirementStatus;

@RestController
@RequestMapping("processes/requirements")
public class ProcessRequirementsRest
{
    @Autowired
    private RequirementsDependenciesJpa requirementsDependenciesJpa;

    @Autowired
    private RequirementsPropertiesJpa requirementsPropertiesJpa;

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public void importRequirements(@RequestParam Long procId, @RequestParam(required = false) List<Long> requirementsId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        List<Requirement> requirements = proc.requirements();

        for (Requirement requirement : requirements)
        {
            proc.removeRequirement(requirement.getRequirementId());
        }

        if (requirementsId == null)
        {
            // No requirement has been added to the process
            return;
        }

        for (Long requirementId : requirementsId)
        {
            Requirement r = DMGame.get().getJpa().requirements.findOne(requirementId);

            if (r == null)
            {
                continue;
            }

            proc.addRequirement(r);
        }
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public List<Requirement> getRequirementsList(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.requirements();
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public Requirement getRequirement(@RequestParam Long procId, @RequestParam Long reqId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.getRequirement(reqId);
    }

    @RequestMapping(value = "/count", method = RequestMethod.GET)
    public int getRequirementsCount(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.getRequirementsCount();
    }

    // Checks if a certain status pertains ALL the requirements
    @RequestMapping(value = "/stablestatus", method = RequestMethod.GET, produces = "text/plain")
    public String getRequirementsStableStatus(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        Map<String, Integer> count = new HashMap<>();
        List<Requirement> requirements = proc.requirements();

        for (Requirement r : requirements)
        {
            RequirementStatus s = RequirementStatus.valueOf(r.getStatus());
            Integer n = count.get(r.getStatus());

            if (n == null)
            {
                n = 0;
            }

            count.put(s.name(), n + 1);
        }

        if (count.size() != 1)
        {
            return "";
        }

        return count.keySet().toArray()[0].toString();
    }

    // Sets a same status to ALL the requirements
    @RequestMapping(value = "/stablestatus", method = RequestMethod.POST, produces = "text/plain")
    public void setRequirementsStableStatus(@RequestParam Long procId,
            @RequestParam(name = "status") String statusString)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        if (mgr == null)
        {
            return;
        }

        try
        {
            RequirementStatus status = RequirementStatus.valueOf(statusString);

            for (Requirement r : mgr.requirements())
            {
                r.setStatus(status.getValue());
                DMGame.get().getJpa().requirements.save(r);
            }
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = "text/plain")
    public String getRequirementsStatus(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        Map<Integer, Integer> count = new HashMap<>();
        count.put(RequirementStatus.Unconfirmed.getValue(), 0);
        count.put(RequirementStatus.Editable.getValue(), 0);
        count.put(RequirementStatus.Confirmed.getValue(), 0);
        count.put(RequirementStatus.Enacted.getValue(), 0);
        count.put(RequirementStatus.Discarded.getValue(), 0);
        List<Requirement> requirements = proc.requirements();

        if (requirements.size() < 1)
        {
            return RequirementStatus.Unconfirmed.name();
        }

        for (Requirement r : requirements)
        {
            Integer n = count.get(r.getStatus());

            if (n == null)
            {
                continue;
            }

            count.put(r.getStatus(), n + 1);
        }

        if (count.get(RequirementStatus.Unconfirmed.getValue()) > 0)
        {
            return RequirementStatus.Unconfirmed.name();
        }

        if (count.get(RequirementStatus.Enacted.getValue()) > 0)
        {
            return RequirementStatus.Enacted.name();
        }

        if (count.get(RequirementStatus.Discarded.getValue()) > 0)
        {
            return RequirementStatus.Discarded.name();
        }

        return RequirementStatus.Confirmed.name();
    }

    @RequestMapping(value = "/statusmap", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Integer> getRequirementsStatusMap(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        Map<String, Integer> count = new HashMap<>();

        for (Requirement r : proc.requirements())
        {
            String str = RequirementStatus.valueOf(r.getStatus()).name();
            Integer n = count.get(str);

            if (n == null)
            {
                n = 0;
            }

            count.put(str, n + 1);
        }

        return count;
    }

    @RequestMapping(value = "/confirm", method = RequestMethod.PUT)
    public void confirmRequirements(@RequestParam Long procId)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        for (Requirement r : mgr.requirements())
        {
            if (r == null)
            {
                continue;
            }

            RequirementStatus oldStatus = RequirementStatus.valueOf(r.getStatus());

            if (RequirementStatus.next(oldStatus).contains(RequirementStatus.Confirmed))
            {
                r.setStatus(RequirementStatus.Confirmed.getValue());
                DMGame.get().getJpa().requirements.save(r);
            }
        }
    }

    @RequestMapping(value = "/dependencies/submit", method = RequestMethod.POST)
    public void setDependencies(@RequestParam Long procId, @RequestBody Map<Long, List<Long>> dependencies)
    {
        for (Long requirementId : dependencies.keySet())
        {
            for (Long dependencyId : dependencies.get(requirementId))
            {
                HRequirementDependency requirementDependency = new HRequirementDependency(requirementId, dependencyId);
                requirementsDependenciesJpa.save(requirementDependency);
            }
        }
    }

    @RequestMapping(value = "/new", method = RequestMethod.POST)
    public Requirement createRequirement(@RequestParam Long procId, @RequestParam String name)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        if (mgr == null)
        {
            return null;
        }

        Requirement r = new Requirement();
        r.setName(name);
        r = DMGame.get().getJpa().requirements.save(r);
        mgr.addRequirement(r);
        return r;
    }

    @RequestMapping(value = "/property/submit", method = RequestMethod.POST)
    public void setProperties(@RequestParam Long procId, @RequestParam Long requirementId,
            @RequestParam String propertyName, @RequestParam String propertyValue)
    {
        HRequirementProperty requirementProperty = new HRequirementProperty(requirementId, propertyName, propertyValue);
        requirementsPropertiesJpa.save(requirementProperty);
    }

    @RequestMapping(value = "/properties", method = RequestMethod.GET)
    public List<HRequirementProperty> getProperties(@RequestParam Long procId, @RequestParam Long requirementId)
    {
        return requirementsPropertiesJpa.findPropertiesByRequirementId(requirementId);
    }

    @RequestMapping(value = "/next", method = RequestMethod.GET, produces = "text/plain")
    public String setNextPhase(@RequestParam Long procId) throws Exception
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);
        String phaseName = mgr.getCurrentPhase();
        DMPhase phase = DMGame.get().getLifecycle().getPhase(phaseName);

        if (phase.getNextPhases().isEmpty())
        {
            throw new RuntimeException("No next phase available");
        }

        // Assume only one next phase is possible

        for (DMPhase n : phase.getNextPhases())
        {
            try
            {
                n.checkPreconditions(mgr);
                n.activate(mgr);
                mgr.setNextPhase(n.getName());
                return n.getName();
            }
            catch (Exception e)
            {
                throw e;
            }
        }

        throw new Exception("No next phase available");
    }

    @RequestMapping(value = "/prev", method = RequestMethod.GET, produces = "text/plain")
    public String setPrevPhase(@RequestParam Long procId) throws Exception
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);
        String phaseName = mgr.getCurrentPhase();
        DMPhase phase = DMGame.get().getLifecycle().getPhase(phaseName);

        if (phase.getPrevPhases().isEmpty())
        {
            throw new RuntimeException("No next phase available");
        }

        // Assume only one next phase is possible

        for (DMPhase n : phase.getPrevPhases())
        {
            try
            {
                n.checkPreconditions(mgr);
                n.activate(mgr);
                mgr.setNextPhase(n.getName());
                return n.getName();
            }
            catch (Exception e)
            {
                throw e;
            }
        }

        throw new Exception("No next phase available");
    }

    @RequestMapping(value = "/edit/collaboratively", method = RequestMethod.GET)
    public List<HActivity> getRequirementsEditingSession(@RequestParam Long procId)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        if (mgr == null)
        {
            return new ArrayList<>();
        }

        return mgr.getOngoingActivities(AccessRequirementsEditingSession.NAME);
    }

    @RequestMapping(value = "/edit/collaboratively", method = RequestMethod.POST)
    public void createRequirementsEditingSession(@RequestParam(required = false) String act, @RequestParam Long procId)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        if (mgr == null)
        {
            return;
        }

        if ("close".equals(act))
        {
            List<HActivity> activities = mgr.getOngoingActivities(AccessRequirementsEditingSession.NAME);

            for (HActivity a : activities)
            {
                mgr.deleteActivity(a);
            }
        }
        else
        {
            for (HProcessMember m : mgr.getProcessMembers())
            {
                mgr.createActivity(AccessRequirementsEditingSession.NAME, m.getUserId());
            }
        }
    }
}