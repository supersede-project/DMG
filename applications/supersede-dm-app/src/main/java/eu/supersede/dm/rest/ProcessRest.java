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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.supersede.dm.ActivityDetails;
import eu.supersede.dm.ActivityEntry;
import eu.supersede.dm.DMGame;
import eu.supersede.dm.DMLibrary;
import eu.supersede.dm.DMMethod;
import eu.supersede.dm.DMPhase;
import eu.supersede.dm.ProcessManager;
import eu.supersede.dm.ProcessRole;
import eu.supersede.dm.PropertyBag;
import eu.supersede.dm.methods.AccessRequirementsEditingSession;
import eu.supersede.fe.security.DatabaseUser;
import eu.supersede.gr.jpa.AlertsJpa;
import eu.supersede.gr.jpa.ReceivedUserRequestsJpa;
import eu.supersede.gr.jpa.RequirementsDependenciesJpa;
import eu.supersede.gr.jpa.RequirementsJpa;
import eu.supersede.gr.jpa.RequirementsPropertiesJpa;
import eu.supersede.gr.model.HActivity;
import eu.supersede.gr.model.HAlert;
import eu.supersede.gr.model.HProcess;
import eu.supersede.gr.model.HProcessCriterion;
import eu.supersede.gr.model.HProcessMember;
import eu.supersede.gr.model.HProperty;
import eu.supersede.gr.model.HReceivedUserRequest;
import eu.supersede.gr.model.HRequirementDependency;
import eu.supersede.gr.model.HRequirementProperty;
import eu.supersede.gr.model.ProcessStatus;
import eu.supersede.gr.model.Requirement;
import eu.supersede.gr.model.RequirementProperties;
import eu.supersede.gr.model.RequirementStatus;
import eu.supersede.gr.model.User;
import eu.supersede.gr.model.ValutationCriteria;

@RestController
@RequestMapping("processes")
public class ProcessRest
{
    @Autowired
    private RequirementsDependenciesJpa requirementsDependenciesJpa;

    @Autowired
    private RequirementsPropertiesJpa requirementsPropertiesJpa;

    @Autowired
    private RequirementsJpa requirementsJpa;

    @Autowired
    private AlertsJpa alertsJpa;

    @Autowired
    private ReceivedUserRequestsJpa receivedUserRequestsJpa;

    // Processes

    static class JqxProcess
    {
        public String id;
        public String name;
        public String state;
        public String date;
        public String objective;
    }

    @RequestMapping(value = "new", method = RequestMethod.POST)
    public Long newProcess()
    {
        return DMGame.get().createEmptyProcess().getId();
    }

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public List<JqxProcess> getProcessList()
    {
        List<JqxProcess> list = new ArrayList<JqxProcess>();

        for (HProcess p : DMGame.get().getJpa().processes.findAll())
        {
            JqxProcess qp = new JqxProcess();
            qp.id = "" + p.getId();
            qp.name = p.getName();
            qp.state = p.getStatus().name();
            qp.objective = p.getObjective();
            qp.date = p.getStartTime().toString();
            list.add(qp);
        }

        return list;
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public String getProcessStatus(@RequestParam Long procId)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        if (mgr == null)
        {
            return "";
        }

        return mgr.getProcessStatus().name();
    }

    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public void getProcessStatus(@RequestParam Long procId, @RequestParam String status)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        if (mgr == null)
        {
            return;
        }

        try
        {
            mgr.setProcessStatus(ProcessStatus.valueOf(status));
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @RequestMapping(value = "/close", method = RequestMethod.POST)
    public void closeProcess(@RequestParam Long procId) throws Exception
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);

        if (mgr == null)
        {
            return;
        }

        if (mgr.getOngoingActivities().size() > 0)
        {
            throw new Exception("In order to close a process, you must close all the activities, first");
        }

        for (HActivity a : mgr.getOngoingActivities())
        {
            DMMethod m = DMLibrary.get().getMethod(a.getMethodName());

            if (m != null)
            {
                // TODO: let the method remote its data?
            }

            mgr.deleteActivity(a);
        }

        HProcess p = DMGame.get().getProcess(procId);

        if (p == null)
        {

            return;
        }

        p.setStatus(ProcessStatus.Closed);

        DMGame.get().getJpa().processes.save(p);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public void deleteProcess(@RequestParam Long procId)
    {
        HProcess p = DMGame.get().getProcess(procId);

        if (p == null)
        {
            return;
        }

        if (p.getStatus() == ProcessStatus.InProgress)
        {
            System.err.println("Can't delete process with id " + procId + ": you must close it first");
            return;
        }

        System.out.println("Deleted process " + procId);
        DMGame.get().deleteProcess(procId);
    }

    // Requirements

    @RequestMapping(value = "/requirements/import", method = RequestMethod.POST)
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

    @RequestMapping(value = "/requirements/list", method = RequestMethod.GET)
    public List<Requirement> getRequirementsList(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.requirements();
    }

    @RequestMapping(value = "/requirements/get", method = RequestMethod.GET)
    public Requirement getRequirement(@RequestParam Long procId, @RequestParam Long reqId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.getRequirement(reqId);
    }

    @RequestMapping(value = "/requirements/count", method = RequestMethod.GET)
    public int getRequirementsCount(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.getRequirementsCount();
    }

    // Checks if a certain status pertains ALL the requirements
    @RequestMapping(value = "/requirements/stablestatus", method = RequestMethod.GET, produces = "text/plain")
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
    @RequestMapping(value = "/requirements/stablestatus", method = RequestMethod.POST, produces = "text/plain")
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

    @RequestMapping(value = "/requirements/status", method = RequestMethod.GET, produces = "text/plain")
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

    @RequestMapping(value = "/requirements/statusmap", method = RequestMethod.GET, produces = "application/json")
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

    @RequestMapping(value = "/requirements/confirm", method = RequestMethod.PUT)
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

    @RequestMapping(value = "/requirements/dependencies/submit", method = RequestMethod.POST)
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

    @RequestMapping(value = "/requirements/new", method = RequestMethod.POST)
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

    @RequestMapping(value = "/requirements/property/submit", method = RequestMethod.POST)
    public void setProperties(@RequestParam Long procId, @RequestParam Long requirementId,
            @RequestParam String propertyName, @RequestParam String propertyValue)
    {
        HRequirementProperty requirementProperty = new HRequirementProperty(requirementId, propertyName, propertyValue);
        requirementsPropertiesJpa.save(requirementProperty);
    }

    @RequestMapping(value = "/requirements/properties", method = RequestMethod.GET)
    public List<HRequirementProperty> getProperties(@RequestParam Long procId, @RequestParam Long requirementId)
    {
        return requirementsPropertiesJpa.findPropertiesByRequirementId(requirementId);
    }

    @RequestMapping(value = "/requirements/next", method = RequestMethod.GET, produces = "text/plain")
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

    @RequestMapping(value = "/requirements/prev", method = RequestMethod.GET, produces = "text/plain")
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

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = "text/plain")
    public String getStatus(@RequestParam Long procId)
    {
        String ret = DMGame.get().getProcessManager(procId).getCurrentPhase();
        if (ret == null)
        {
            ret = DMGame.get().getLifecycle().getInitPhase().getName();
        }
        return ret;
    }

    // Criteria

    @RequestMapping(value = "/criteria/import", method = RequestMethod.POST)
    public void importCriteria(@RequestParam Long procId, @RequestParam(required = false) List<Long> idlist)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        List<HProcessCriterion> processCriteria = proc.getProcessCriteria();

        for (HProcessCriterion processCriterion : processCriteria)
        {
            proc.removeCriterion(processCriterion.getCriterionId(), processCriterion.getSourceId(),
                    processCriterion.getProcessId());
        }

        if (idlist == null)
        {
            // No criterion has been added to the process.
            return;
        }

        for (Long cid : idlist)
        {
            ValutationCriteria c = DMGame.get().getCriterion(cid);
            proc.addCriterion(c);
        }
    }

    @RequestMapping(value = "/criteria/list", method = RequestMethod.GET)
    public List<Long> getCriteriaList(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        List<Long> list = new ArrayList<>();

        for (ValutationCriteria c : proc.getCriteria())
        {
            list.add(c.getCriteriaId());
        }

        return list;
    }

    @RequestMapping(value = "/criteria/list/detailed", method = RequestMethod.GET)
    public List<HProcessCriterion> getCriteriaObjectList(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.getProcessCriteria();
    }

    // Users

    @RequestMapping(value = "/users/import", method = RequestMethod.POST)
    public void importUsers(@RequestParam Long procId, @RequestParam(required = false) List<Long> idlist)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        List<HProcessMember> processMembers = proc.getProcessMembers();

        for (HProcessMember processMember : processMembers)
        {
            proc.removeProcessMember(processMember.getId(), processMember.getUserId(), processMember.getProcessId());
        }

        if (idlist == null)
        {
            // No user has been added to the process.
            return;
        }

        for (Long userid : idlist)
        {
            proc.addProcessMember(userid, ProcessRole.User.name());
        }
    }

    @RequestMapping(value = "/users/list", method = RequestMethod.GET)
    public List<Long> getUserList(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        List<Long> list = new ArrayList<>();

        for (HProcessMember member : proc.getProcessMembers())
        {
            list.add(member.getUserId());
        }

        return list;
    }

    @RequestMapping(value = "/users/list/detailed", method = RequestMethod.GET)
    public List<User> getUserObjectList(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        List<User> list = new ArrayList<>();

        for (HProcessMember member : proc.getProcessMembers())
        {
            User u = DMGame.get().getJpa().users.findOne(member.getUserId());

            if (u != null)
            {
                list.add(u);
            }
        }

        return list;
    }

    // Activities

    @RequestMapping(value = "/available_activities", method = RequestMethod.GET)
    public List<ActivityEntry> getNextActivities(Long procId)
    {
        return DMGame.get().getProcessManager(procId).findNextActivities(DMLibrary.get().methods());
    }

    @RequestMapping(value = "/activities/list", method = RequestMethod.GET)
    public List<ActivityDetails> getActivityList(Authentication auth)
    {
        List<ActivityDetails> list = new ArrayList<>();
        List<HActivity> activities = DMGame.get()
                .getPendingActivities(((DatabaseUser) auth.getPrincipal()).getUserId());

        for (HActivity a : activities)
        {
            ActivityDetails d = new ActivityDetails();
            d.setActivityId(a.getId());
            d.setMethodName(a.getMethodName());
            d.setProcessId(a.getProcessId());
            d.setUserId(a.getUserId());
            ProcessManager mgr = DMGame.get().getProcessManager(a.getProcessId());
            DMMethod m = DMLibrary.get().getMethod(a.getMethodName());

            if (m != null)
            {
                d.setUrl(m.getPage(mgr));
                list.add(d);
            }

            PropertyBag bag = mgr.getProperties(a);

            for (HProperty p : bag.properties())
            {
                d.setProperty(p.getKey(), p.getValue());
            }
        }

        return list;
    }

    @RequestMapping(value = "/activities/groups", method = RequestMethod.GET)
    public Map<String, List<Long>> getActivityGroups(@RequestParam Long procId)
    {
        ProcessManager mgr = DMGame.get().getProcessManager(procId);
        List<HActivity> activities = mgr.getOngoingActivities();
        Map<String, List<Long>> map = new HashMap<>();

        for (HActivity a : activities)
        {
            List<Long> list = map.get(a.getMethodName());

            if (list == null)
            {
                list = new ArrayList<>();
                map.put(a.getMethodName(), list);
            }

            list.add(a.getUserId());
        }

        return map;
    }

    // Alerts

    @RequestMapping(value = "/alerts/import", method = RequestMethod.POST)
    public void importAlerts(@RequestParam Long procId, @RequestParam(required = false) List<String> alertsId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        List<HAlert> alerts = proc.getAlerts();

        for (HAlert alert : alerts)
        {
            proc.removeAlert(alert.getId());
        }

        if (alertsId == null)
        {
            // No alert has been added to the process.
            return;
        }

        for (String alertId : alertsId)
        {
            HAlert alert = DMGame.get().getJpa().alerts.findOne(alertId);

            if (alert == null)
            {
                continue;
            }

            proc.addAlert(alert);
        }
    }

    @RequestMapping(value = "/alerts/list", method = RequestMethod.GET)
    public List<HAlert> getAlertsList(@RequestParam Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        return proc.getAlerts();
    }

    @RequestMapping(value = "/methods/{methodname}/{action}", method = RequestMethod.POST)
    public void postToMethod(@PathVariable String methodName, @PathVariable String action,
            @RequestParam Map<String, String> args)
    {
        DMMethod m = DMLibrary.get().getMethod(methodName);
        if (m != null)
        {
            // m.post( action, args );
        }
    }

    @RequestMapping(value = "/requirements/edit/collaboratively", method = RequestMethod.POST)
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

    @RequestMapping(value = "/requirements/edit/collaboratively", method = RequestMethod.GET)
    public List<HActivity> getRequirementsEditingSession(@RequestParam Long procId)
    {

        ProcessManager mgr = DMGame.get().getProcessManager(procId);
        if (mgr == null)
        {
            return new ArrayList<>();
        }

        return mgr.getOngoingActivities(AccessRequirementsEditingSession.NAME);

    }

    @RequestMapping(value = "/alerts/convert", method = RequestMethod.PUT)
    public void convertAlertToRequirement(@RequestParam String alertId, Long procId)
    {
        ProcessManager proc = DMGame.get().getProcessManager(procId);
        HAlert alert = alertsJpa.findOne(alertId);
        System.out.println("Converting alert " + alertId + " to a requirement");
        List<HReceivedUserRequest> requests = receivedUserRequestsJpa.findRequestsForAlert(alertId);

        if (requests == null || requests.size() == 0)
        {
            System.out.println("No user requests for alert " + alertId + ", no requirement added");
            return;
        }

        for (HReceivedUserRequest request : requests)
        {
            Requirement requirement = new Requirement();
            requirement.setName(request.getDescription());
            requirement.setDescription("Features:");
            Requirement savedRequirement = requirementsJpa.save(requirement);
            proc.addRequirement(savedRequirement);

            requirementsPropertiesJpa.save(new HRequirementProperty(savedRequirement.getRequirementId(),
                    RequirementProperties.CLASSIFICATION, request.getClassification()));
            requirementsPropertiesJpa.save(new HRequirementProperty(savedRequirement.getRequirementId(),
                    RequirementProperties.ACCURACY, "" + request.getAccuracy()));
            requirementsPropertiesJpa.save(new HRequirementProperty(savedRequirement.getRequirementId(),
                    RequirementProperties.POSITIVE_SENTIMENT, "" + request.getPositiveSentiment()));
            requirementsPropertiesJpa.save(new HRequirementProperty(savedRequirement.getRequirementId(),
                    RequirementProperties.NEGATIVE_SENTIMENT, "" + request.getNegativeSentiment()));
            requirementsPropertiesJpa.save(new HRequirementProperty(savedRequirement.getRequirementId(),
                    RequirementProperties.OVERALL_SENTIMENT, "" + request.getOverallSentiment()));
        }

        System.out.println("Discarding alert " + alertId);
        alertsJpa.delete(alert);
    }
}