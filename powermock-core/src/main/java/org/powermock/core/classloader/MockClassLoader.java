/*
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.powermock.core.classloader;

import org.powermock.core.transformers.ClassWrapper;
import org.powermock.core.transformers.ClassWrapperFactory;
import org.powermock.core.transformers.MockTransformer;
import org.powermock.tests.utils.IgnorePackagesExtractor;
import org.powermock.core.transformers.MockTransformerChain;
import org.powermock.core.transformers.javassist.support.ClassWrapperFactoryImpl;

import java.security.ProtectionDomain;
import java.util.List;

/**
 * <p>
 * The classloader loads and modified all classes except:
 * </p>
 * <ol>
 * <li>system classes. They are deferred to system classloader</li>
 * <li>classes that locate in packages that specified as packages to ignore with using {@link #addIgnorePackage(String...)}</li>
 * </ol>
 * <p>
 * Testing frameworks classes are loaded, but not modified.
 * </p>
 * <p>
 * The classloader uses list of {@link MockTransformer} to modify classes during loading.
 * </p>
 *
 * @author Johan Haleby
 * @author Jan Kronquist
 * @author Artur Zagretdinov
 * @see ClassLoader#getSystemClassLoader()
 * @see IgnorePackagesExtractor
 */
public abstract class MockClassLoader extends DeferSupportingClassLoader {
    
    /**
     * Pass this string to the constructor to indicate that all classes should
     * be modified.
     */
    public static final String MODIFY_ALL_CLASSES = "*";
    
    protected ClassMarker classMarker;
    protected ClassWrapperFactory classWrapperFactory;
    private MockTransformerChain mockTransformerChain;
    
    /**
     * Creates a new instance of the  based on the
     * following parameters:
     *
     * @param classesToMock   The classes that must be modified to prepare for testability.
     * @param packagesToDefer Classes in these packages will be defered to the system
     *                        class-loader.
     */
    protected MockClassLoader(String[] classesToMock, String[] packagesToDefer) {
        this(new MockClassLoaderConfiguration(classesToMock, packagesToDefer));
    }
    
    /**
     * Creates a new instance of the  based on the
     * following parameters:
     *
     * @param configuration  The configuration of class loader. Configuration contains information about classes
     *                       which should be loaded by class loader, defer to system and mocked.
     * @see MockClassLoaderConfiguration
     */
    protected MockClassLoader(MockClassLoaderConfiguration configuration) {
        super(MockClassLoader.class.getClassLoader(), configuration);
        classWrapperFactory = new ClassWrapperFactoryImpl();
    }
    
    @Override
    protected Class<?> loadModifiedClass(String className) throws ClassFormatError, ClassNotFoundException {
        Class<?> loadedClass;
        
        Class<?> deferClass = deferTo.loadClass(className);
        if (getConfiguration().shouldMockClass(className)) {
            loadedClass = loadMockClass(className, deferClass.getProtectionDomain());
        } else {
            loadedClass = loadUnmockedClass(className, deferClass.getProtectionDomain());
        }
        return loadedClass;
    }
    
    public void setMockTransformerChain(MockTransformerChain mockTransformerChain) {
        this.mockTransformerChain = mockTransformerChain;
    }
    
    protected abstract Class<?> loadUnmockedClass(String name,
                                                  ProtectionDomain protectionDomain) throws ClassFormatError, ClassNotFoundException;
    
    protected abstract Class<?> loadMockClass(String name, ProtectionDomain protectionDomain);
    
    protected <T> ClassWrapper<T> transformClass(ClassWrapper<T> wrappedType) throws Exception {
        wrappedType = mockTransformerChain.transform(wrappedType);
        
        if (classMarker != null) {
            classMarker.mark(wrappedType);
        }
        return wrappedType;
    }
}
