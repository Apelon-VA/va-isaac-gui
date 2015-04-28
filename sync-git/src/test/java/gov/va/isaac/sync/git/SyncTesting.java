/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.sync.git;

import gov.va.isaac.AppContext;
import gov.va.isaac.interfaces.sync.MergeFailOption;
import gov.va.isaac.interfaces.sync.ProfileSyncI;
import java.io.File;
import java.util.HashMap;

/**
 * {@link SyncTesting}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class SyncTesting
{
	public static void main(String[] args) throws Exception
	{
		//Configure Java logging into log4j2
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

		ProfileSyncI ssg = AppContext.getService(SyncServiceGIT.class);
		File localFolder = new File("/mnt/SSD/scratch/gitTesting");
		ssg.setRootLocation(localFolder);
		
		String username = "username";
		String password = "password";

		ssg.linkAndFetchFromRemote("https://github.com/" + username + "/test.git", username, password);
		ssg.linkAndFetchFromRemote("ssh://" + username + "@csfe.aceworkspace.net:29418/testrepo", username, password);
		ssg.addUntrackedFiles();
		System.out.println("UpdateCommitAndPush result: " + ssg.updateCommitAndPush("mergetest2", username, password, MergeFailOption.FAIL, (String[])null));

		ssg.removeFiles("b");

		System.out.println("Update from remote result: " + ssg.updateFromRemote(username, password, MergeFailOption.FAIL));

		HashMap<String, MergeFailOption> resolutions = new HashMap<>();
		resolutions.put("b", MergeFailOption.KEEP_REMOTE);
		System.out.println("resolve merge failures result: " + ssg.resolveMergeFailures(resolutions));
		System.out.println("UpdateCommitAndPush result: " + ssg.updateCommitAndPush("mergetest2", username, password, MergeFailOption.FAIL, (String[])null));
	}
}
