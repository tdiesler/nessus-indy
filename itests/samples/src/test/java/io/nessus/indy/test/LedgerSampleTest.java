/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.nessus.indy.test;

import static io.nessus.indy.utils.IndyConstants.PROTOCOL_VERSION;
import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;
import static org.hyperledger.indy.sdk.ledger.Ledger.signAndSubmitRequest;
import static org.junit.Assert.assertEquals;

import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidJSONParameters;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.indy.utils.PoolUtils;


public class LedgerSampleTest {

	Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void testLedger() throws Exception {
		
		logInfo("LD_LIBRARY_PATH: {}", System.getenv("LD_LIBRARY_PATH"));
		
		logInfo("Ledger sample -> started");

		String trusteeSeed = "000000000000000000000000Trustee1";

		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PROTOCOL_VERSION).get();

		// 1. Create ledger config from genesis txn file
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();

		// 2. Create and Open My Wallet
		String myWalletConfig = new JSONObject().put("id", "myWallet").toString();
		String myWalletCredentials = new JSONObject().put("key", "my_wallet_key").toString();
		Wallet.createWallet(myWalletConfig, myWalletCredentials).get();
		Wallet myWallet = Wallet.openWallet(myWalletConfig, myWalletCredentials).get();

		// 3. Create and Open Trustee Wallet
		String trusteeWalletConfig = new JSONObject().put("id", "theirWallet").toString();
		String trusteeWalletCredentials = new JSONObject().put("key", "trustee_wallet_key").toString();
		Wallet.createWallet(trusteeWalletConfig, trusteeWalletCredentials).get();
		Wallet trusteeWallet = Wallet.openWallet(trusteeWalletConfig, trusteeWalletCredentials).get();

		// 4. Create My Did
		CreateAndStoreMyDidResult createMyDidResult = Did.createAndStoreMyDid(myWallet, "{}").get();
		String myDid = createMyDidResult.getDid();
		String myVerkey = createMyDidResult.getVerkey();

		// 5. Create Did from Trustee1 seed
		DidJSONParameters.CreateAndStoreMyDidJSONParameter theirDidJson =
				new DidJSONParameters.CreateAndStoreMyDidJSONParameter(null, trusteeSeed, null, null);

		CreateAndStoreMyDidResult createTheirDidResult = Did.createAndStoreMyDid(trusteeWallet, theirDidJson.toJson()).get();
		String trusteeDid = createTheirDidResult.getDid();

		// 6. Build Nym Request
		String nymRequest = buildNymRequest(trusteeDid, myDid, myVerkey, null, null).get();

		// 7. Trustee Sign Nym Request
		String nymResponseJson = signAndSubmitRequest(pool, trusteeWallet, trusteeDid, nymRequest).get();

		JSONObject nymResponse = new JSONObject(nymResponseJson);

		assertEquals(myDid, nymResponse.getJSONObject("result").getJSONObject("txn").getJSONObject("data").getString("dest"));
		assertEquals(myVerkey, nymResponse.getJSONObject("result").getJSONObject("txn").getJSONObject("data").getString("verkey"));

		// 8. Close and delete My Wallet
		myWallet.closeWallet().get();
		Wallet.deleteWallet(myWalletConfig, myWalletCredentials).get();

		// 9. Close and delete Their Wallet
		trusteeWallet.closeWallet().get();
		Wallet.deleteWallet(trusteeWalletConfig, trusteeWalletCredentials).get();

		// 10. Close Pool
		pool.closePoolLedger().get();

		// 11. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();

		logInfo("Ledger sample -> completed");
	}

	private void logInfo(String msg, Object... args) {
		log.info(msg, args);
	}
}
