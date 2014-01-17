/*
 * Copyright 2007 the original author or authors.
 *
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
 */

package org.gradle.api.tasks.compile;

import org.gradle.api.AntBuilder;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.internal.tasks.compile.*;
import org.gradle.api.internal.tasks.compile.daemon.CompilerDaemonManager;
import org.gradle.api.internal.tasks.compile.incremental.SelectiveCompilation;
import org.gradle.api.internal.tasks.compile.incremental.SelectiveJavaCompiler;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.internal.Factory;
import org.gradle.util.DeprecationLogger;

import java.io.File;
import java.util.Set;

/**
 * Compiles Java source files.
 *
 * @deprecated This class has been replaced by {@link JavaCompile}.
 */
@Deprecated
public class Compile extends AbstractCompile {
    private Compiler<JavaCompileSpec> javaCompiler;
    private File dependencyCacheDir;
    private final CompileOptions compileOptions = new CompileOptions();
    private Set<File> sourceDirs;

    public Compile() {
        if (!(this instanceof JavaCompile)) {
            DeprecationLogger.nagUserOfReplacedTaskType("Compile", "JavaCompile task type");
        }
        Factory<AntBuilder> antBuilderFactory = getServices().getFactory(AntBuilder.class);
        JavaCompilerFactory inProcessCompilerFactory = new InProcessJavaCompilerFactory();
        ProjectInternal projectInternal = (ProjectInternal) getProject();
        CompilerDaemonManager compilerDaemonManager = getServices().get(CompilerDaemonManager.class);
        JavaCompilerFactory defaultCompilerFactory = new DefaultJavaCompilerFactory(projectInternal, antBuilderFactory, inProcessCompilerFactory, compilerDaemonManager);
        javaCompiler = new DelegatingJavaCompiler(defaultCompilerFactory);
    }

    @TaskAction
    protected void compile(IncrementalTaskInputs inputs) {
        SelectiveJavaCompiler compiler = new SelectiveJavaCompiler(javaCompiler);
        SelectiveCompilation selectiveCompilation = new SelectiveCompilation(inputs, getSource(), getClasspath(), getDestinationDir(),
                getClassTreeCache(), compiler, sourceDirs);

        DefaultJavaCompileSpec spec = new DefaultJavaCompileSpec();
        spec.setSource(selectiveCompilation.getSource());
        spec.setDestinationDir(getDestinationDir());
        spec.setClasspath(selectiveCompilation.getClasspath());
        spec.setDependencyCacheDir(getDependencyCacheDir());
        spec.setSourceCompatibility(getSourceCompatibility());
        spec.setTargetCompatibility(getTargetCompatibility());
        spec.setCompileOptions(compileOptions);
        WorkResult result = compiler.execute(spec);
        setDidWork(result.getDidWork());
        selectiveCompilation.compilationComplete();
    }

    private File getClassTreeCache() {
        return new File(getProject().getBuildDir(), "class-tree.bin");
    }

    @OutputDirectory
    public File getDependencyCacheDir() {
        return dependencyCacheDir;
    }

    public void setDependencyCacheDir(File dependencyCacheDir) {
        this.dependencyCacheDir = dependencyCacheDir;
    }

    /**
     * Returns the compilation options.
     *
     * @return The compilation options.
     */
    @Nested
    public CompileOptions getOptions() {
        return compileOptions;
    }

    public Compiler<JavaCompileSpec> getJavaCompiler() {
        return javaCompiler;
    }

    public void setJavaCompiler(Compiler<JavaCompileSpec> javaCompiler) {
        this.javaCompiler = javaCompiler;
    }

    public void setSource(SourceDirectorySet source) {
        this.sourceDirs = source.getSrcDirs(); //so that we can infer the input class -> output class mapping. This is very naive.
        super.setSource(source);
    }
}
