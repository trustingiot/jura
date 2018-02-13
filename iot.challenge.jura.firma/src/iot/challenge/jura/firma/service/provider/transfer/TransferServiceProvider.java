package iot.challenge.jura.firma.service.provider.transfer;

import iot.challenge.jura.firma.service.TransferService;
import iot.challenge.jura.util.trait.ActionRecorder;
import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transfer;
import jota.pow.SpongeFactory;
import jota.utils.IotaAPIUtils;
import jota.utils.TrytesConverter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;

import cfb.pearldiver.PearlDiverLocalPoW;

/**
 * TransferService provider
 */
public class TransferServiceProvider implements TransferService, ActionRecorder, ConfigurableComponent {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.jura.firma.transfer";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected IotaAPI api;
	protected Options options;
	protected String address;
	protected List<Transfer> transfers;
	protected Consumer<SendTransferResponse> callback;
	protected ExecutorService transferWorker;
	protected Future<?> transferHandle;

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Activating", this::activate, properties);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Updating", this::update, properties);
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Deactivating", this::deactivate);
	}

	////
	//
	// Functionality
	//
	//
	protected void activate(Map<String, Object> properties) {
		createOptions(properties);
	}

	protected void createOptions(Map<String, Object> properties) {
		options = new Options(properties);
		updateAPI();
	}

	protected void updateAPI() {
		if (shouldUpdateAPI()) {
			api = new IotaAPI.Builder()
					.localPoW(new PearlDiverLocalPoW())
					.protocol(options.getIotaNodeProtocol())
					.host(options.getIotaNodeHost())
					.port(options.getIotaNodePort())
					.build();
			obtainAddress();
		}
	}

	protected boolean shouldUpdateAPI() {
		if (api == null)
			return true;

		if (!api.getProtocol().equals(options.getIotaNodeProtocol()))
			return true;

		if (!api.getHost().equals(options.getIotaNodeHost()))
			return true;

		if (!api.getPort().equals(options.getIotaNodePort()))
			return true;

		return false;
	}

	protected void obtainAddress() {
		try {
			address = IotaAPIUtils.newAddress(
					options.getIotaSeed(), // Seed
					2, // Security
					0, // Address
					true, // Add checksum
					SpongeFactory.create(SpongeFactory.Mode.KERL)); // Curl instance
		} catch (ArgumentException exception) {
			address = null;
		}
	}

	protected void update(Map<String, Object> properties) {
		shutdownWorker();
		createOptions(properties);
	}

	protected void shutdownWorker() {
		synchronized (this) {
			if (transferHandle != null) {
				transferHandle.cancel(true);
				transferHandle = null;
			}

			if (transferWorker != null) {
				transferWorker.shutdown();
				transferWorker = null;
			}
		}
	}

	protected void deactivate() {
		shutdownWorker();
	}

	////
	//
	// Transfer service
	//
	//
	@Override
	public boolean sendingTransfer() {
		return transferHandle != null;
	}

	@Override
	public void transfer(String address, String message, Consumer<SendTransferResponse> callback) {
		transfer(new Transfer(address, 0, TrytesConverter.toTrytes(message), ""), callback);
	}

	@Override
	public void transfer(String address, String message) {
		transfer(address, message, null);
	}

	@Override
	public void transfer(Transfer transfer, Consumer<SendTransferResponse> callback) {
		boolean executed = false;

		synchronized (this) {
			executed = executeTransfer(transfer, callback);
		}

		if (callback != null && !executed)
			callback.accept(null);
	}

	@Override
	public void transfer(Transfer transfer) {
		transfer(transfer, null);
	}

	protected boolean executeTransfer(Transfer transfer, Consumer<SendTransferResponse> callback) {
		if (sendingTransfer())
			return false;

		if (transferWorker == null)
			transferWorker = Executors.newScheduledThreadPool(1);

		prepareTransfer(transfer, callback);
		transferHandle = transferWorker.submit(this::sendPreparedTransfer);
		return true;
	}

	protected void prepareTransfer(Transfer transfer, Consumer<SendTransferResponse> callback) {
		this.transfers = Arrays.asList(transfer);
		this.callback = callback;
	}

	protected void sendPreparedTransfer() {
		SendTransferResponse response = null;

		try {
			long time = System.currentTimeMillis();
			response = sendToIOTA(transfers);
			logTransfer(time, response);
		} catch (Exception exception) {
			error("IOTA transfer fail");
		}

		transferHandle = null;

		if (callback != null)
			callback.accept(response);

	}

	protected SendTransferResponse sendToIOTA(List<Transfer> transfers) throws ArgumentException {
		return api.sendTransfer(
				options.getIotaSeed(), // Seed
				2, // Security
				9, // Depth
				14, // Min weight
				transfers, // Transfers
				null, // Inputs
				null, // Remainders
				false); // Validate inputs
	}

	protected void logTransfer(long time, SendTransferResponse transfer) {
		info("Transaction https://thetangle.org/transaction/{} done ({} seconds)",
				transfer.getTransactions().get(0).getHash(),
				(System.currentTimeMillis() - time) / 1000);
	}
}
