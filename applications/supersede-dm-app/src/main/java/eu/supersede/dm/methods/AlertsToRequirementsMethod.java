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

package eu.supersede.dm.methods;

import java.util.ArrayList;
import java.util.List;

import eu.supersede.dm.DMCondition;
import eu.supersede.dm.DMMethod;
import eu.supersede.dm.DMObjective;
import eu.supersede.dm.DMOption;
import eu.supersede.dm.DMRoleSpec;
import eu.supersede.dm.ProcessManager;

public class AlertsToRequirementsMethod implements DMMethod
{
    private static final String NAME = "Convert Alerts To Requirements";
    private static final String PAGE = "convert_alerts_to_requirements";

    private List<DMRoleSpec> list;
    private List<DMOption> options;

    public AlertsToRequirementsMethod()
    {
        list = new ArrayList<>();
        options = new ArrayList<>();
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public DMObjective getObjective()
    {
        return DMObjective.PrioritizeRequirements;
    }

    @Override
    public List<DMRoleSpec> getRoleList()
    {
        return list;
    }

    public String getPage(String step)
    {
        return "";
    }

    @Override
    public List<DMOption> getOptions()
    {
        return this.options;
    }

    public void setOption(String optName, String optValue)
    {
        // TODO
    }

    public void init(ProcessManager status)
    {
        // String pid = executor.startBPMN( "supersedeAHPDM" );
        // status.setProperty( "pid", pid );
    }

    @Override
    public List<DMCondition> preconditions()
    {
        List<DMCondition> list = new ArrayList<DMCondition>();

        list.add(new DMCondition()
        {
            @Override
            public boolean isTrue(ProcessManager mgr)
            {
                // return mgr.getAlerts().size() > 0;
                // Temporarily disable this activity until the conversion from alert to requirement is fixed
                return false;
            }
        });

        return list;
    }

    @Override
    public String getPage(ProcessManager mgr)
    {
        return PAGE;
    }

    @Override
    public String getDescription(ProcessManager arg0)
    {
        return "Convert Alerts To Requirements";
    }

    @Override
    public String getLabel(ProcessManager arg0)
    {
        return "Convert Alerts To Requirements";
    }
}