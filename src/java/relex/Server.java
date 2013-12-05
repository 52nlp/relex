/*
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
 *
 * Copyright (c) 2008, 2009, 2013 Linas Vepstas <linasvepstas@gmail.com>
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
import java.util.HashSet;
import java.util.Map;
import relex.output.SimpleView;
import relex.output.OpenCogScheme;
import relex.Version;

/**
 * The Server class provides a very simple socket-based parse server.
 * It will listen for plain-text input sentences on port 4444, and will
 * generate OpenCog output.
 *
 * It is intended that this server be used by OpenCog agents to process
 * text; the text is sent from opencog to this server, and the returned
 * parses are then further processed by OpenCog.
 */

public class Server
{
	private int listen_port;

	public Server()
	{
		listen_port = 4444;
	}

	public static void main(String[] args)
	{
		int listen_port = 4444;
		int max_parses = 1;
		boolean relex_on = false;
		boolean link_on = false;
		boolean anaphora_on = false;
		boolean verbose = false;
		String lang = "en";
		String usageString = "RelEx server (designed for OpenCog interaction).\n" +
			"Given a sentence, it returns a parse in opencog-style scheme format.\n" +
			" -p number  \t Port number to listen on (default: 4444)\n" +
			" --port num \t Port number to listen on (default: 4444)\n" +
			" --lang lang\t Set langauge (default: en)\n" +
			" -n number  \t Max number of parses to return (default: 1)\n" +
			" --relex    \t Output RelEx relations (default)\n" +
			" --link     \t Output Link Grammar Linkages\n" +
			" --anaphora \t Output anaphore references\n" +
			" --verbose  \t Print parse output to server stdout.\n";

		HashSet<String> flags = new HashSet<String>();
		flags.add("-h");
		flags.add("--anaphora");
		flags.add("--help");
		flags.add("--link");
		flags.add("--relex");
		flags.add("--verbose");
		HashSet<String> opts = new HashSet<String>();
		opts.add("-n");
		opts.add("-p");
		opts.add("--lang");
		opts.add("--port");
		Map<String,String> commandMap = CommandLineArgParser.parse(args, opts, flags);

		try
		{
			String opt = commandMap.get("--lang");
			if (opt != null) lang = opt;

			opt = commandMap.get("-n");
			if (opt != null) max_parses = Integer.parseInt(opt);

			opt = commandMap.get("-p");
			if (opt != null) listen_port = Integer.parseInt(opt);

			opt = commandMap.get("--port");
			if (opt != null) listen_port = Integer.parseInt(opt);
		}
		catch (Exception e)
		{
			System.err.println("Unrecognized parameter.");
			System.err.println(usageString);
			System.exit(1);
		}

		if (commandMap.get("-h") != null ||
		    commandMap.get("--help") != null)
		{
			System.err.println(usageString);
			System.exit(0);
		}
		if (commandMap.get("--anaphora") != null) anaphora_on = true;
		if (commandMap.get("--link") != null) link_on = true;
		if (commandMap.get("--relex") != null) relex_on = true;

		if (commandMap.get("--verbose") != null)
		{
			System.err.println("Info: Verbose server mode set.");
			verbose = true;
		}

		System.err.println("Info: Version: " + Version.getVersion());

		// -----------------------------------------------------------------
		// After parsing the commmand arguments, set up the assorted classes.
		RelationExtractor re = new RelationExtractor(false);
		re.setLanguage(lang);
		re.setMaxParses(max_parses);
		OpenCogScheme opencog = new OpenCogScheme();
		Server s = new Server();
		s.listen_port = listen_port;
		ServerSocket listen_sock = null;

		if (!relex_on && !link_on)
		{
			// By default just export RelEx output.
			relex_on = true;
		}
		if (anaphora_on)
		{
			System.err.println("Info: Anaphora output on.");
			opencog.setShowAnaphora(anaphora_on);
		}
		if (link_on)
		{
			System.err.println("Info: Link grammar output on.");
			opencog.setShowLinkage(link_on);
		}
		if (relex_on)
		{
			System.err.println("Info: RelEx output on.");
			opencog.setShowRelex(relex_on);
		}
		else
		{
			re.do_apply_algs = false;
		}

		// -----------------------------------------------------------------
		// Socket setup
		try
		{
			listen_sock = new ServerSocket(s.listen_port);
		}
		catch (IOException e)
		{
			System.err.println("Error: Listen failed on port " + s.listen_port);
			System.exit(-1);
		}
		System.err.println("Info: Listening on port " + s.listen_port);

		// -----------------------------------------------------------------
		// Main loop
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
				System.err.println("Error: Accept failed");
				continue;
			}

			System.err.println("Info: Socket accept");
			BufferedReader in = new BufferedReader(new InputStreamReader(ins));
			PrintWriter out = new PrintWriter(outs, true);

			try {
				String line = in.readLine();
				if (line == null)
					continue;
				System.err.println("Info: recv input: \"" + line + "\"");
				Sentence sntc = re.processSentence(line);
				if (sntc.getParses().size() == 0)
				{
					out.println("; NO PARSES");
					continue;
				}
				int pn;
				for (pn = 0; pn < max_parses; pn++)
				{
					ParsedSentence parse = sntc.getParses().get(pn);

					// Print the phrase string ... handy for debugging.
					out.println("; " + parse.getPhraseString());

					if (verbose)
					{
						String fin = SimpleView.printRelationsAlt(parse);
						System.out.print(fin);
					}
					opencog.setParse(parse);
					out.println(opencog.toString());
				}

				// Add a special tag to tell the cog server that it's
				// just recieved a brand new sentence. The OpenCog scheme
				// code depends on this being visible, in order to find
				// the new sentence.
				out.println("(ListLink (stv 1 1)");
				out.println("   (AnchorNode \"# New Parsed Sentence\")");
				out.println("   (SentenceNode \"" + sntc.getID() + "\")");
				out.println(")");

				out.println("; END OF SENTENCE");

				out.close();
				System.err.println("Info: Closed printer");
			}
			catch (IOException e)
			{
				System.err.println("Error: Processing input failed");
				continue;
			}

			try
			{
				out_sock.close();
				System.err.println("Info: Closed socket");
			}
			catch (IOException e)
			{
				System.err.println("Error: Socket close failed");
				continue;
			}
		}
	}
}

