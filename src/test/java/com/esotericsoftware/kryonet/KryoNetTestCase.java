/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryonet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;

abstract public class KryoNetTestCase extends TestCase {
	static public String host = "localhost";
	static public int tcpPort = 54555, udpPort = 54777;

	private ArrayList<Thread> threads = new ArrayList<Thread>();
	ArrayList<EndPoint> endPoints = new ArrayList<EndPoint>();
	private Timer timer;
	boolean fail;

	public KryoNetTestCase () {}

	protected void setUp () throws Exception {
		System.out.println("---- " + getClass().getSimpleName());
		timer = new Timer();
	}

	protected void tearDown () throws Exception {
		timer.cancel();
	}

	public void startEndPoint (EndPoint endPoint) {
		endPoints.add(endPoint);
		Thread thread = new Thread(endPoint, endPoint.getClass().getSimpleName());
		threads.add(thread);
		thread.start();
	}

	public void stopEndPoints () {
		stopEndPoints(0);
	}

	public void stopEndPoints (int stopAfterMillis) {
		timer.schedule(new TimerTask() {
			public void run () {
				for (EndPoint endPoint : endPoints)
					endPoint.stop();
				endPoints.clear();
			}
		}, stopAfterMillis);
	}

	public void waitForThreads (int stopAfterMillis) {
		if (stopAfterMillis > 10000) throw new IllegalArgumentException("stopAfterMillis must be < 10000");
		stopEndPoints(stopAfterMillis);
		waitForThreads();
	}

	public void waitForThreads () {
		fail = false;
		TimerTask failTask = new TimerTask() {
			public void run () {
				stopEndPoints();
				fail = true;
			}
		};
		timer.schedule(failTask, 11000);
		while (true) {
			for (Iterator<Thread> iter = threads.iterator(); iter.hasNext();) {
				Thread thread = iter.next();
				if (!thread.isAlive()) iter.remove();
			}
			if (threads.isEmpty()) break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignored) {
			}
		}
		failTask.cancel();
		if (fail) fail("Test did not complete in a timely manner.");
		// Give sockets a chance to close before starting the next test.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {
		}
	}
}
