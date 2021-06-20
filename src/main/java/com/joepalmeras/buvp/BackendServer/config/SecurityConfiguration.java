package com.joepalmeras.buvp.BackendServer.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableJdbcHttpSession
@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private DataSource dataSource;
	
	@Override
    protected void configure(HttpSecurity http) throws Exception {
	
		http.authorizeRequests()
	      .anyRequest().authenticated()
	      .and()
	      	.formLogin()
	      .and()
	      	.logout()
	      .and()
	      	.httpBasic();
		
//		http
//        .authorizeRequests()
////          .antMatchers("/h2-console/**").permitAll()
////          .antMatchers("/index/**").permitAll()//.hasRole("USER")
////          .antMatchers("/**").permitAll()//.hasRole("USER")
//          .and()
//       .formLogin()
//           .and()
//       .logout();
//		
//        http.csrf().disable();
//        http.headers().frameOptions().disable();
    }

	@Override
	public void configure(WebSecurity web) throws Exception {
	   web
	     .ignoring()
	        .antMatchers("/h2-console/**");
	}
	
	@Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) 
      throws Exception {
//		auth.inMemoryAuthentication()
        auth.jdbcAuthentication().dataSource(dataSource)
        .withDefaultSchema()
			.withUser("user1")
		      .password(passwordEncoder().encode("user1"))
		      .roles("USER")
		    .and()
		      .withUser("user2")
		      .password(passwordEncoder().encode("user2"))
		      .roles("USER");
    }
	
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
	
//	@Bean
//	public EmbeddedDatabase dataSource() {
//		return new EmbeddedDatabaseBuilder()
//				.setType(EmbeddedDatabaseType.H2).addScript("classpath:/schema-h2.sql").build();
//	}
//
//	@Bean
//	public PlatformTransactionManager transactionManager(DataSource dataSource) {
//		return new DataSourceTransactionManager(dataSource);
//	}
	
}
