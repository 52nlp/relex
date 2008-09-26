/*
 * Copyright 2008 Novamente LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package relex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.Socket;
// import relex.output.SimpleView;
// import relex.frame.Frame;
import relex.output.OpenCogXML;

public class Server
{
	private int listen_port;

	public Server()
	{
		listen_port = 4444;
	}

	public static void main(String[] args)
	{
		RelationExtractor r = new RelationExtractor(false);
		// Frame frame = new Frame();
		OpenCogXML opencog = new OpenCogXML();
		Server s = new Server();
		ServerSocket listen_sock = null;
		try {
			listen_sock = new ServerSocket(s.listen_port);
		} catch (IOException e) {
			System.out.println("Listen failed on port " + s.listen_port);
			System.exit(-1);
		}

		while(true)
		{
			Socket out_sock = null;
			OutputStream outs = null;
			InputStream ins = null;
			try {
				out_sock = listen_sock.accept();
				ins = out_sock.getInputStream();
				outs = out_sock.getOutputStream();
			} catch (IOException e) {
				System.out.println("Accept failed");
				continue;
			}

System.out.println("duude got accept");
			BufferedReader in = new BufferedReader(new InputStreamReader(ins));
			PrintWriter out = new PrintWriter(outs, true);

			try {
	 			while(out_sock.isConnected()) {
					String line = in.readLine();
					if (line == null)
						break;
					Sentence sntc = r.processSentence(line);
					if (sntc.getParses().size() == 0)
					{
						out.println("no parses");
						continue;
					}
					ParsedSentence p = sntc.getParses().get(0);

					/*
					out.println(p.getPhraseString());

					String fin = SimpleView.printRelationsAlt(p);
					String[] fout = frame.process(fin);
					for (int i=0; i < fout.length; i++) {
						out.println(fout[i]);
					}
					*/
					
					opencog.setParse(p);
					out.println(opencog.toString());
				}

				out.close();
			} catch (IOException e) {
				System.out.println("Processing input failed");
				continue;
			}

			try {
				out_sock.close();
			} catch (IOException e) {
				System.out.println("Socket close failed");
				continue;
			}
		}
	}
}


