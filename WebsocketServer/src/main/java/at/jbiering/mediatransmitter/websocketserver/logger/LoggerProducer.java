package at.jbiering.mediatransmitter.websocketserver.logger;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class LoggerProducer {

	
	@Produces
	public Logger produceLogger(InjectionPoint ij) {
		return LoggerFactory.getLogger(ij.getBean().getBeanClass());
	}
}
