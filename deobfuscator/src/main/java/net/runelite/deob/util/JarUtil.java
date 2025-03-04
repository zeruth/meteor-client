/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.deob.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import net.runelite.asm.ClassFile;
import net.runelite.asm.ClassGroup;
import net.runelite.asm.objectwebasm.NonloadingClassWriter;
import net.runelite.asm.visitors.ClassFileVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

public class JarUtil
{

	public static ClassGroup load(File jarfile)
	{
		return load(jarfile, false);
	}

	public static ClassGroup load(File jarfile, boolean skip)
	{
		ClassGroup group = new ClassGroup();

		try (JarFile jar = new JarFile(jarfile))
		{
			for (Enumeration<JarEntry> it = jar.entries(); it.hasMoreElements();)
			{
				JarEntry entry = it.nextElement();

				if (!entry.getName().endsWith(".class") || (skip && entry.getName().contains("bouncycastle")))
				{
					continue;
				}

				InputStream is = jar.getInputStream(entry);

				ClassReader reader = new ClassReader(is);
				ClassFileVisitor cv = new ClassFileVisitor();

				reader.accept(cv, ClassReader.SKIP_FRAMES);

				group.addClass(cv.getClassFile());
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		group.initialize();

		return group;
	}

	public static ClassFile loadClass(byte[] bytes)
	{
		ClassReader reader = new ClassReader(bytes);
		ClassFileVisitor cv = new ClassFileVisitor();
		reader.accept(cv, ClassReader.SKIP_FRAMES);
		return cv.getClassFile();
	}

	public static ClassGroup loadClasses(Collection<File> files) throws IOException
	{
		return loadClasses(files, false);
	}

	public static ClassGroup loadClasses(Collection<File> files, boolean skip) throws IOException
	{
		final ClassGroup group = new ClassGroup();

		for (File file : files)
		{
			if (!file.getName().endsWith(".class") || (skip && file.getName().contains("bouncycastle")))
			{
				continue;
			}

			try (InputStream is = new FileInputStream(file))
			{
				ClassReader reader = new ClassReader(is);
				ClassFileVisitor cv = new ClassFileVisitor();

				reader.accept(cv, ClassReader.SKIP_FRAMES);

				group.addClass(cv.getClassFile());
			}
		}

		group.initialize();

		return group;
	}

	public static void save(ClassGroup group, File jarfile)
	{
		try (JarOutputStream jout = new JarOutputStream(new FileOutputStream(jarfile)))
		{
			for (ClassFile cf : group.getClasses())
			{
				JarEntry entry = new JarEntry(cf.getName() + ".class");
				entry.setTime(-1);
				try {
					jout.putNextEntry(entry);

					byte[] data = writeClass(group, cf);

					jout.write(data);
					jout.closeEntry();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static byte[] writeClass(ClassGroup group, ClassFile cf)
	{
		ClassWriter writer = new NonloadingClassWriter(group, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		CheckClassAdapter cca = new CheckClassAdapter(writer, false);

		cf.accept(cca);

		byte[] data = writer.toByteArray();

		validateDataFlow(cf.getName(), data);

		return data;
	}

	private static void validateDataFlow(String name, byte[] data)
	{
		try
		{
			ClassReader cr = new ClassReader(data);
			ClassWriter cw = new ClassWriter(cr, 0);
			ClassVisitor cv = new CheckClassAdapter(cw, true);
			cr.accept(cv, 0);
		}
		catch (Exception ex)
		{
		}
	}
}
