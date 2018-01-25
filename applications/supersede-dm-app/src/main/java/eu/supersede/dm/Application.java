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

/**
 * @author Andrea Sosi
 **/

package eu.supersede.dm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication(
		exclude = DataSourceAutoConfiguration.class)
@ComponentScan(basePackages = {
		"eu.supersede.dm", 
		"eu.supersede.gr", 
		"eu.supersede.gr.jpa", 
		"eu.supersede.gr.model", 
		"eu.supersede.fe",
		"eu.supersede.dm.ga",
		"eu.supersede.dm.ga.db",
		"eu.supersede.dm.ga.jpa",
		"eu.supersede.gr.logics",
		"eu.supersede.dm",
		"eu.supersede.dm.depcheck", 
		"eu.supersede.analysis"
		})
@EnableGlobalMethodSecurity( 
		securedEnabled = true, prePostEnabled = true )
@EnableJpaRepositories(basePackages={
		"eu.supersede.dm.jpa", 
		"eu.supersede.fe.notification.jpa", 
		"eu.supersede.gr.jpa",
		"eu.supersede.dm.ga.db",
		"eu.supersede.dm.ga.jpa"
		})
@EnableScheduling
@EnableRedisHttpSession
public class Application extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}