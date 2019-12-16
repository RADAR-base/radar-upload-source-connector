/**
 * Copyright (c) 2009-2018, Newcastle University, UK.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.radarbase.connect.upload.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.radarbase.connect.upload.converter.axivity.newcastle.CwaCsvInputStream;

/**
 * Command-line utility for converting CWA files to CSV files, demonstrates the CwaCsvInputStream class.
 * All data output in CSV to stdout, all other output to stderr.
 * @author Dan Jackson
 */
public class CwaConverter {

	/**
	 * Simple command-line conversion utility to demonstrate the CwaCsvInputStream class.
	 * @param args Command-line arguments
	 */
	public static void main(String[] args) {
		try {
			// Default parameter values
			boolean help = false;
			String inputFile = null;
			String outputFile = null;
			int start = 0, skip = 1, count = -1;
			int options = 0;

			// Read command-line parameters
			int positional = 0;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("--help") || args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("-?")) {
					help = true;
				} else if (args[i].equalsIgnoreCase("-in") && i + 1 < args.length) {
					inputFile = args[++i];
				} else if (args[i].equalsIgnoreCase("-out") && i + 1 < args.length) {
					outputFile = args[++i];
				} else if (args[i].equalsIgnoreCase("-start") && i + 1 < args.length) {
					start = Integer.parseInt(args[++i]);
				} else if (args[i].equalsIgnoreCase("-skip") && i + 1 < args.length) {
					skip = Integer.parseInt(args[++i]);
				} else if (args[i].equalsIgnoreCase("-count") && i + 1 < args.length) {
					count = Integer.parseInt(args[++i]);
				} else if (args[i].equalsIgnoreCase("-light")) {
					options |= CwaCsvInputStream.OPTIONS_LIGHT;
				} else if (args[i].equalsIgnoreCase("-temp")) {
					options |= CwaCsvInputStream.OPTIONS_TEMP;
				} else if (args[i].equalsIgnoreCase("-batt")) {
					options |= CwaCsvInputStream.OPTIONS_BATT;
				} else if (args[i].equalsIgnoreCase("-events")) {
					options |= CwaCsvInputStream.OPTIONS_EVENTS;
				} else if (args[i].equalsIgnoreCase("-metadata")) {
					options |= CwaCsvInputStream.OPTIONS_METADATA;
				} else if (positional == 0) {
					// First positional argument is the input file
					inputFile = args[i];
					positional++;
				} else {
					System.err.println("ERROR: Unknown argument (or missing value): " + args[i]);
					help = true;
				}
			}

			// Display help if required
			if (help) {
				System.err.println("Usage: CwaConverter [[-in] CWA-DATA.CWA] [-out CWA-DATA.CSV] [-start 0] [-skip 1] [-count -1] [-light] [-temp] [-batt] [-events] [-metadata]");
				System.exit(-1);
			}

			// Establish input and output streams
			InputStream in = System.in;
			PrintStream out = System.out;
			if (inputFile != null) {
				in = new FileInputStream(inputFile);
			}
			if (outputFile != null) {
				out = new PrintStream(new FileOutputStream(outputFile), true);
			}

			System.out.println("Options " + options);
			// Create the filter stream around the input stream
			BufferedReader filter = new BufferedReader(new InputStreamReader(new CwaCsvInputStream(in, start, skip, count, options)));

			// While not EOF, read and output the CSV lines
			String line;

//            while ((line = filter.readLine()) != null) {
//                out.println(line);
//            }
			for(int i =0; i<20; i++) {
                line = filter.readLine();
				out.println(line);
			}

			// Close the streams
			filter.close();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}

}
