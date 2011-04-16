/*
 * Copyright 2006 - 2011 
 *     Julien Baudry	<julien.baudry@graphstream-project.org>
 *     Antoine Dutot	<antoine.dutot@graphstream-project.org>
 *     Yoann Pigné		<yoann.pigne@graphstream-project.org>
 *     Guilhelm Savin	<guilhelm.savin@graphstream-project.org>
 * 
 * This file is part of GraphStream <http://graphstream-project.org>.
 * 
 * GraphStream is a library whose purpose is to handle static or dynamic
 * graph, create them from scratch, file or any source and display them.
 * 
 * This program is free software distributed under the terms of two licenses, the
 * CeCILL-C license that fits European law, and the GNU Lesser General Public
 * License. You can  use, modify and/ or redistribute the software under the terms
 * of the CeCILL-C license as circulated by CEA, CNRS and INRIA at the following
 * URL <http://www.cecill.info> or under the terms of the GNU LGPL as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C and LGPL licenses and that you accept their terms.
 */
package org.graphstream.tool;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.graphstream.algorithm.generator.Generator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.ElementSink;
import org.graphstream.stream.file.FileSink;

/**
 * Helper to generate graph in command line.
 * 
 * @author Guilhelm Savin
 * 
 */
public class Generate implements ToolsCommon {

	public static void main(String... args) {
		GeneratorType type = GeneratorType.PREFERENTIAL_ATTACHMENT;
		SinkFormat format = SinkFormat.DGS;
		String[][] generatorOptions = null;
		String[][] formatOptions = null;

		int size = 0;
		int ite = 0;
		long delay = 0;
		boolean loop = true;
		int iteration = 0;
		boolean export = false;
		boolean force = false;

		OutputStream out = System.out;
		Generator gen = null;
		FileSink sink = null;
		ElementCounter counter = new ElementCounter();

		String path = null;

		if (args == null) {
			usage(System.err);
			System.exit(1);
		}

		Tools.removeShortcuts(args, shortcuts);

		for (int k = 0; k < args.length; k++) {
			if (args[k].matches("^--\\w+(-\\w+)*(=.*)?$")) {
				int idx = args[k].indexOf('=');
				String key;
				String value;

				if (idx < 0) {
					key = args[k].substring(2);
					value = null;
				} else {
					key = args[k].substring(2, idx);
					value = args[k].substring(idx + 1).trim();
				}

				if (value != null && value.matches("^\".*\"$"))
					value = value.substring(1, value.length() - 1);

				if (key.equals("type")) {
					try {
						type = GeneratorType.valueOf(value);
					} catch (IllegalArgumentException e) {
						System.err.printf("Invalid generator type : \"%s\".\n",
								value);
						System.exit(1);
					}
				} else if (key.equals("format")) {
					try {
						format = SinkFormat.valueOf(value);
					} catch (IllegalArgumentException e) {
						System.err.printf("Invalid output format : \"%s\".\n",
								value);
						System.exit(1);
					}
				} else if (key.equals("size")) {
					if (value.matches("^\\d+$")) {
						size = Integer.parseInt(value);
					} else {
						System.err.printf("Invalid size : %s.\n", value);
						System.exit(1);
					}
				} else if (key.equals("iteration")) {
					if (value.matches("^\\d+$")) {
						iteration = Integer.parseInt(value);
					} else {
						System.err.printf("Invalid iteration count : %s.\n",
								value);
						System.exit(1);
					}
				} else if (key.equals("delay")) {
					if (value.matches("^\\d+$")) {
						delay = Long.parseLong(value);
					} else {
						System.err.printf("Invalid delay : %s.\n", value);
						System.exit(1);
					}
				} else if (key.equals("generator-options")) {
					try {
						generatorOptions = Tools.getKeyValue(value);
					} catch (IllegalArgumentException e) {
						System.err
								.printf("Invalid options : %s.\nFormat is : key=value.\n",
										e.getMessage());
						System.exit(1);
					} catch (NullPointerException e) {
						System.err
								.printf("--generator-options is done but value is null.\n");
					}
				} else if (key.equals("output-options")) {
					try {
						formatOptions = Tools.getKeyValue(value);
					} catch (IllegalArgumentException e) {
						System.err
								.printf("Invalid options : %s.\nFormat is : key=value.\n",
										e.getMessage());
						System.exit(1);
					} catch (NullPointerException e) {
						System.err
								.printf("--output-options is done but value is null.\n");
					}
				} else if (key.equals("export")) {
					export = true;
				} else if (key.equals("force")) {
					force = true;
				} else {
					System.err.printf("Unknown option : \"%s\".\n", key);
					usage(System.err);
					System.exit(1);
				}
			} else if (args[k].matches("--help|-h")) {
				usage(System.out);
				System.exit(0);
			} else if (args[k].startsWith("--")) {
				System.err.printf("Unknown option : \"%s\"\n", args[k]);
				usage(System.err);
				System.exit(1);
			} else if (path == null) {
				path = args[k];
			} else {
				System.err
						.printf("Just one path is allowed. Previous is \"%s\".\n",
								path);
				usage(System.err);
				System.exit(1);
			}
		}

		if (path != null) {
			try {
				out = new FileOutputStream(path);
			} catch (IOException e) {
				System.err.printf("Error with file \"%s\".\n", path);
				System.err.printf("Message is : %s.\n", e.getMessage());
				System.exit(1);
			}
		}

		if (size == 0 && iteration == 0 && !force) {
			System.err.printf("Neither --size or --iteration have been defined.\n");
			System.err.printf("Add --force to bypass this protection.\n");
			System.exit(1);
		}

		if (!export && !format.hasDynamicSupport() && !force) {
			System.err.printf("The format \"%s\" is not dynamic. ",
					format.name());
			System.err
					.printf("Use --export to export the whole at the end of the generation ");
			System.err
					.printf("or use --force to force the dynamic generation.\n");
			System.exit(1);
		}

		if (formatOptions != null) {
			for (int i = 0; i < formatOptions.length; i++) {
				if (!format.isValidOption(formatOptions[i][0])) {
					System.err.printf(
							"Invalid option \"%s\" for output. Options are:\n",
							formatOptions[i][0]);
					for (int k = 0; k < format.getOptionCount(); k++)
						System.err.printf("- %s\n", format.getOption(k));
					System.exit(1);
				}
			}
		}

		Graph exportGraph = null;

		gen = Tools.generatorFor(type, generatorOptions);
		sink = Tools.sinkFor(format, formatOptions);
		gen.addElementSink(counter);

		if (export) {
			exportGraph = new DefaultGraph("export");
			gen.addSink(exportGraph);
		} else {
			gen.addSink(sink);

			try {
				sink.begin(out);
			} catch (IOException e1) {
				System.err.printf("Cannot begin the sink.\n");
				System.err.printf("Message is : %s.\n", e1.getMessage());
				System.exit(1);
			}
		}

		gen.begin();

		do {
			gen.nextEvents();

			ite++;

			loop = (iteration <= 0 || ite < iteration)
					&& (size <= 0 || counter.getNodeCount() < size);

			try {
				if (!export)
					sink.flush();
			} catch (IOException e1) {
				// Ignore
			}

			if (delay > 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					loop = false;
				}
			}
		} while (loop);

		gen.end();

		try {
			if (export) {
				sink.writeAll(exportGraph, out);
			} else {
				sink.end();
			}
		} catch (IOException e) {
			System.err.printf("Cannot end the sink.\n");
			System.err.printf("Message is : %s.\n", e.getMessage());
			System.exit(1);
		}
	}

	private static final String[][] shortcuts = {
			{ "-pa", "--type=PREFERENTIAL_ATTACHMENT" },
			{ "-dm", "--type=DOROGOVTSEV_MENDES" }, { "-f", "--type=FULL" },
			{ "-r", "--type=RANDOM" }, { "-g", "--type=GRID" },
			{ "-dgs", "--format=DGS" }, { "-dot", "--format=DOT" },
			{ "-gml", "--format=GML" }, { "-tikz", "--format=TIKZ" },
			{ "-i", "--format=IMAGES" }, { "-e", "--export" },
			{ "-H", "--size=100" }, { "-K", "--size=1000" },
			{ "-M", "--size=1000000" } };

	/**
	 * Usage of this class.
	 */
	public static void usage(PrintStream out) {
		out.printf("Usage: java %s [OPTIONS] [OUT]\n\n",
				Generate.class.getName());
		out.printf("with OUT is the output file path, or empty for stdout.\n");
		out.printf("with OPTIONS:\n");
		out.printf("\t--type=X                    : type of generator\n");
		for (GeneratorType t : GeneratorType.values())
			out.printf("\t\t%s%n", t.name());
		out.printf("\t--format=X                  : output format\n");
		for (SinkFormat f : SinkFormat.values())
			out.printf("\t\t%s%n", f.name());
		out.printf("\t--iteration=xxx             : iteration of the generator\n");
		out.printf("\t--size=xxx                  : size of graph\n");
		out.printf("\t--generator-options=\"...\" : options given to the generator\n");
		out.printf("\t--output-options=\"...\"    : options given to the output\n");
		out.printf("\t--delay=xxx                 : delay between iteration (ms)\n");
		out.printf("\t--export                    : export the graph after the generation.\n");
		out.printf("Shortcuts :\n");
		for (int i = 0; i < shortcuts.length; i++)
			out.printf("\t\"%s\"\t: \"%s\"\n", shortcuts[i][0], shortcuts[i][1]);
	}

	private static class ElementCounter implements ElementSink {

		int nodes = 0;
		int edges = 0;

		public int getNodeCount() {
			return nodes;
		}

		/*
		 * public int getEdgeCount() { return edges; }
		 */
		public void nodeAdded(String sourceId, long timeId, String nodeId) {
			nodes++;
		}

		public void nodeRemoved(String sourceId, long timeId, String nodeId) {
			nodes--;
		}

		public void edgeAdded(String sourceId, long timeId, String edgeId,
				String fromNodeId, String toNodeId, boolean directed) {
			edges++;
		}

		public void edgeRemoved(String sourceId, long timeId, String edgeId) {
			edges--;
		}

		public void graphCleared(String sourceId, long timeId) {
			nodes = 0;
			edges = 0;
		}

		public void stepBegins(String sourceId, long timeId, double step) {
		}
	}
}