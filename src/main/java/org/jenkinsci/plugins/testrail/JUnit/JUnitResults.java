/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.testrail.JUnit;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import jenkins.MasterToSlaveFileCallable;

/**
 * Created by Drew on 3/24/2014.
 */
public class JUnitResults {
    private FilePath baseDir;
    private PrintStream logger;
    //private String[] Files;
    private List<TestSuite> Suites;

    public JUnitResults(FilePath baseDir, String fileMatchers, PrintStream logger) throws IOException, JAXBException, InterruptedException {
        this.baseDir = baseDir;
        this.logger = logger;
        slurpTestResults(fileMatchers);
    }

    public void slurpTestResults(String fileMatchers) throws IOException, JAXBException, InterruptedException {
        Suites = new ArrayList<TestSuite>();
        JAXBContext jaxbSuiteContext = JAXBContext.newInstance(TestSuite.class);
        JAXBContext jaxbSuitesContext = JAXBContext.newInstance(TestSuites.class);
        final Unmarshaller jaxbSuiteUnmarshaller = jaxbSuiteContext.createUnmarshaller();
        final Unmarshaller jaxbSuitesUnmarshaller = jaxbSuitesContext.createUnmarshaller();

        final DirScanner scanner = new DirScanner.Glob(fileMatchers, null);
        logger.println("Scanning " + baseDir);

        baseDir.act(new MasterToSlaveFileCallable<Void>() {
            private static final long serialVersionUID = 1L;

            public Void invoke(File f, VirtualChannel channel) throws IOException {
                logger.println("processing folder " + f.getName());
                scanner.scan(f, new FileVisitor() {
                    @Override
                    public void visit(File file, String s) throws IOException {
                        logger.println("processing FILE " + file.getName());
                        if (file.getName().equals("test-results.xml")) {
                            Scanner xmlScanner = new Scanner(file);
                            xmlScanner.useDelimiter("\n");
                            while (xmlScanner.hasNext()) {
                                logger.println(xmlScanner.next());
                            }
                            xmlScanner.close();
                        }

                        try {
                            TestSuites suites = (TestSuites) jaxbSuitesUnmarshaller.unmarshal(file);
                            logger.println(suites.getSuites().size() + "HEARTON");
                            logger.println(suites.getSuites().get(0).toString() + "sshufflethedeck");
                            if (suites.hasSuites()) {
                                for (TestSuite suite : suites.getSuites()) {
                                    Suites.add(suite);
                                }
                            }
                        } catch (ClassCastException e) {
                            try {
                                TestSuite suite = (TestSuite) jaxbSuiteUnmarshaller.unmarshal(file);
                                logger.println(suite.toString() + "IN CATCH" + e);
                                Suites.add(suite);
                           } catch (JAXBException ex) {
                                logger.println("IN CATCH OF CATCH" + ex);
                               ex.printStackTrace();
                           }
                        } catch (JAXBException exc) {
                            logger.println("IDK WHERE IS IT" + exc);
                            exc.printStackTrace();
                        }
                    }
                });
                return null;
            }
        });
    }
    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    public List<TestSuite> getSuites() {
        return this.Suites;
    }

    //public String[] getFiles() { return this.Files.clone(); }
}
