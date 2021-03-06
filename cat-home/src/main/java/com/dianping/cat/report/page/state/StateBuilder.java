package com.dianping.cat.report.page.state;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.Constants;
import com.dianping.cat.consumer.state.StateAnalyzer;
import com.dianping.cat.consumer.state.model.entity.Machine;
import com.dianping.cat.consumer.state.model.entity.StateReport;
import com.dianping.cat.home.router.entity.DefaultServer;
import com.dianping.cat.report.page.model.spi.ModelService;
import com.dianping.cat.service.ModelRequest;
import com.dianping.cat.service.ModelResponse;
import com.dianping.cat.system.config.RouterConfigManager;

public class StateBuilder {

	@Inject
	private RouterConfigManager m_routerManager;

	@Inject(type = ModelService.class, value = StateAnalyzer.ID)
	private ModelService<StateReport> m_stateService;

	private List<String> queryAllServers() {
		List<String> strs = new ArrayList<String>();
		String backUpServer = m_routerManager.getRouterConfig().getBackupServer();
		List<DefaultServer> servers = m_routerManager.getRouterConfig().getDefaultServers();

		for (DefaultServer server : servers) {
			strs.add(server.getId());
		}
		strs.add(backUpServer);
		return strs;
	}

	public String buildStateMessage(long date, String ip) {
		StateReport report = queryHourlyReport(date, ip);

		int realSize = report.getMachines().size();
		List<String> servers = queryAllServers();
		int excepeted = servers.size();
		Set<String> errorServers = new HashSet<String>();

		if (realSize != excepeted) {
			for (String serverIp : servers) {
				if (report.getMachines().get(serverIp) == null) {
					errorServers.add(serverIp);
				}
			}
		}
		for (Machine machine : report.getMachines().values()) {
			if (machine.getTotalLoss() > 300 * 10000) {
				errorServers.add(machine.getIp());
			}
		}

		if (errorServers.size() > 0) {
			return errorServers.toString();
		} else {
			return null;
		}
	}

	private StateReport queryHourlyReport(long date, String ip) {
		String domain = Constants.CAT;
		ModelRequest request = new ModelRequest(domain, date) //
		      .setProperty("ip", ip);

		if (m_stateService.isEligable(request)) {
			ModelResponse<StateReport> response = m_stateService.invoke(request);

			return response.getModel();
		} else {
			throw new RuntimeException("Internal error: no eligable sql service registered for " + request + "!");
		}
	}
}
