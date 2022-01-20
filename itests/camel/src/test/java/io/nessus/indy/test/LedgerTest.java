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

import org.hyperledger.indy.sdk.pool.Pool;
import org.junit.Test;


public class LedgerTest {

	@Test
	public void testLedger() throws Exception {
		
		System.out.println("Ledger sample -> started");

		// Set protocol version 2
		Pool.setProtocolVersion(PoolUtils.PROTOCOL_VERSION).get();
		
		// 1. Create ledger config from genesis txn file
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();
		
		// ...
		
		// 10. Close Pool
		pool.closePoolLedger().get();

		// 11. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();

		System.out.println("Ledger sample -> completed");
	}
}
