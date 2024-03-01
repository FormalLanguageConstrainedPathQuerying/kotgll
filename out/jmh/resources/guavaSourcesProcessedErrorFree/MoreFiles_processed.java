/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.Traverser;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;

/**
 * Static utilities for use with {@link Path} instances, intended to complement {@link Files}.
 *
 * <p>Many methods provided by Guava's {@code Files} class for {@link java.io.File} instances are
 * now available via the JDK's {@link java.nio.file.Files} class for {@code Path} - check the JDK's
 * class if a sibling method from {@code Files} appears to be missing from this class.
 *
 * @since 21.0
 * @author Colin Decker
 */
@J2ktIncompatible
@GwtIncompatible
@J2ObjCIncompatible 
@ElementTypesAreNonnullByDefault
public final class MoreFiles {

  private MoreFiles() {}

  /**
   * Returns a view of the given {@code path} as a {@link ByteSource}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned source and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#READ READ} option.
   */
  public static ByteSource asByteSource(Path path, OpenOption... options) {
    return new PathByteSource(path, options);
  }

  private static final class PathByteSource extends
      ByteSource
  {

    private static final LinkOption[] FOLLOW_LINKS = {};

    private final Path path;
    private final OpenOption[] options;
    private final boolean followLinks;

    private PathByteSource(Path path, OpenOption... options) {
      this.path = checkNotNull(path);
      this.options = options.clone();
      this.followLinks = followLinks(this.options);
    }

    private static boolean followLinks(OpenOption[] options) {
      for (OpenOption option : options) {
        if (option == NOFOLLOW_LINKS) {
          return false;
        }
      }
      return true;
    }

    @Override
    public InputStream openStream() throws IOException {
      return Files.newInputStream(path, options);
    }

    private BasicFileAttributes readAttributes() throws IOException {
      return Files.readAttributes(
          path,
          BasicFileAttributes.class,
          followLinks ? FOLLOW_LINKS : new LinkOption[] {NOFOLLOW_LINKS});
    }

    @Override
    public Optional<Long> sizeIfKnown() {
      BasicFileAttributes attrs;
      try {
        attrs = readAttributes();
      } catch (IOException e) {
        return Optional.absent();
      }

      if (attrs.isDirectory() || attrs.isSymbolicLink()) {
        return Optional.absent();
      }

      return Optional.of(attrs.size());
    }

    @Override
    public long size() throws IOException {
      BasicFileAttributes attrs = readAttributes();

      if (attrs.isDirectory()) {
        throw new IOException("can't read: is a directory");
      } else if (attrs.isSymbolicLink()) {
        throw new IOException("can't read: is a symbolic link");
      }

      return attrs.size();
    }

    @Override
    public byte[] read() throws IOException {
      try (SeekableByteChannel channel = Files.newByteChannel(path, options)) {
        return ByteStreams.toByteArray(Channels.newInputStream(channel), channel.size());
      }
    }

    @Override
    public CharSource asCharSource(Charset charset) {
      if (options.length == 0) {
        return new AsCharSource(charset) {
          @SuppressWarnings("FilesLinesLeak") 
          @Override
          public Stream<String> lines() throws IOException {
            return Files.lines(path, charset);
          }
        };
      }

      return super.asCharSource(charset);
    }

    @Override
    public String toString() {
      return "MoreFiles.asByteSource(" + path + ", " + Arrays.toString(options) + ")";
    }
  }

  /**
   * Returns a view of the given {@code path} as a {@link ByteSink}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned sink and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
   * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
   * WRITE} options.
   */
  public static ByteSink asByteSink(Path path, OpenOption... options) {
    return new PathByteSink(path, options);
  }

  private static final class PathByteSink extends ByteSink {

    private final Path path;
    private final OpenOption[] options;

    private PathByteSink(Path path, OpenOption... options) {
      this.path = checkNotNull(path);
      this.options = options.clone();
    }

    @Override
    public OutputStream openStream() throws IOException {
      return Files.newOutputStream(path, options);
    }

    @Override
    public String toString() {
      return "MoreFiles.asByteSink(" + path + ", " + Arrays.toString(options) + ")";
    }
  }

  /**
   * Returns a view of the given {@code path} as a {@link CharSource} using the given {@code
   * charset}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned source and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#READ READ} option.
   */
  public static CharSource asCharSource(Path path, Charset charset, OpenOption... options) {
    return asByteSource(path, options).asCharSource(charset);
  }

  /**
   * Returns a view of the given {@code path} as a {@link CharSink} using the given {@code charset}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned sink and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
   * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
   * WRITE} options.
   */
  public static CharSink asCharSink(Path path, Charset charset, OpenOption... options) {
    return asByteSink(path, options).asCharSink(charset);
  }

  /**
   * Returns an immutable list of paths to the files contained in the given directory.
   *
   * @throws NoSuchFileException if the file does not exist <i>(optional specific exception)</i>
   * @throws NotDirectoryException if the file could not be opened because it is not a directory
   *     <i>(optional specific exception)</i>
   * @throws IOException if an I/O error occurs
   */
  public static ImmutableList<Path> listFiles(Path dir) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      return ImmutableList.copyOf(stream);
    } catch (DirectoryIteratorException e) {
      throw e.getCause();
    }
  }

  /**
   * Returns a {@link Traverser} instance for the file and directory tree. The returned traverser
   * starts from a {@link Path} and will return all files and directories it encounters.
   *
   * <p>The returned traverser attempts to avoid following symbolic links to directories. However,
   * the traverser cannot guarantee that it will not follow symbolic links to directories as it is
   * possible for a directory to be replaced with a symbolic link between checking if the file is a
   * directory and actually reading the contents of that directory.
   *
   * <p>If the {@link Path} passed to one of the traversal methods does not exist or is not a
   * directory, no exception will be thrown and the returned {@link Iterable} will contain a single
   * element: that path.
   *
   * <p>{@link DirectoryIteratorException} may be thrown when iterating {@link Iterable} instances
   * created by this traverser if an {@link IOException} is thrown by a call to {@link
   * #listFiles(Path)}.
   *
   * <p>Example: {@code MoreFiles.fileTraverser().depthFirstPreOrder(Paths.get("/"))} may return the
   * following paths: {@code ["/", "/etc", "/etc/config.txt", "/etc/fonts", "/home", "/home/alice",
   * ...]}
   *
   * @since 23.5
   */
  public static Traverser<Path> fileTraverser() {
    return Traverser.forTree(MoreFiles::fileTreeChildren);
  }

  private static Iterable<Path> fileTreeChildren(Path dir) {
    if (Files.isDirectory(dir, NOFOLLOW_LINKS)) {
      try {
        return listFiles(dir);
      } catch (IOException e) {
        throw new DirectoryIteratorException(e);
      }
    }
    return ImmutableList.of();
  }

  /**
   * Returns a predicate that returns the result of {@link java.nio.file.Files#isDirectory(Path,
   * LinkOption...)} on input paths with the given link options.
   */
  public static Predicate<Path> isDirectory(LinkOption... options) {
    final LinkOption[] optionsCopy = options.clone();
    return new Predicate<Path>() {
      @Override
      public boolean apply(Path input) {
        return Files.isDirectory(input, optionsCopy);
      }

      @Override
      public String toString() {
        return "MoreFiles.isDirectory(" + Arrays.toString(optionsCopy) + ")";
      }
    };
  }

  /** Returns whether or not the file with the given name in the given dir is a directory. */
  private static boolean isDirectory(
      SecureDirectoryStream<Path> dir, Path name, LinkOption... options) throws IOException {
    return dir.getFileAttributeView(name, BasicFileAttributeView.class, options)
        .readAttributes()
        .isDirectory();
  }

  /**
   * Returns a predicate that returns the result of {@link java.nio.file.Files#isRegularFile(Path,
   * LinkOption...)} on input paths with the given link options.
   */
  public static Predicate<Path> isRegularFile(LinkOption... options) {
    final LinkOption[] optionsCopy = options.clone();
    return new Predicate<Path>() {
      @Override
      public boolean apply(Path input) {
        return Files.isRegularFile(input, optionsCopy);
      }

      @Override
      public String toString() {
        return "MoreFiles.isRegularFile(" + Arrays.toString(optionsCopy) + ")";
      }
    };
  }

  /**
   * Returns true if the files located by the given paths exist, are not directories, and contain
   * the same bytes.
   *
   * @throws IOException if an I/O error occurs
   * @since 22.0
   */
  public static boolean equal(Path path1, Path path2) throws IOException {
    checkNotNull(path1);
    checkNotNull(path2);
    if (Files.isSameFile(path1, path2)) {
      return true;
    }

    /*
     * Some operating systems may return zero as the length for files denoting system-dependent
     * entities such as devices or pipes, in which case we must fall back on comparing the bytes
     * directly.
     */
    ByteSource source1 = asByteSource(path1);
    ByteSource source2 = asByteSource(path2);
    long len1 = source1.sizeIfKnown().or(0L);
    long len2 = source2.sizeIfKnown().or(0L);
    if (len1 != 0 && len2 != 0 && len1 != len2) {
      return false;
    }
    return source1.contentEquals(source2);
  }

  /**
   * Like the unix command of the same name, creates an empty file or updates the last modified
   * timestamp of the existing file at the given path to the current system time.
   */
  @SuppressWarnings("GoodTime") 
  public static void touch(Path path) throws IOException {
    checkNotNull(path);

    try {
      Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
    } catch (NoSuchFileException e) {
      try {
        Files.createFile(path);
      } catch (FileAlreadyExistsException ignore) {
      }
    }
  }

  /**
   * Creates any necessary but nonexistent parent directories of the specified path. Note that if
   * this operation fails, it may have succeeded in creating some (but not all) of the necessary
   * parent directories. The parent directory is created with the given {@code attrs}.
   *
   * @throws IOException if an I/O error occurs, or if any necessary but nonexistent parent
   *     directories of the specified file could not be created.
   */
  public static void createParentDirectories(Path path, FileAttribute<?>... attrs)
      throws IOException {
    Path normalizedAbsolutePath = path.toAbsolutePath().normalize();
    Path parent = normalizedAbsolutePath.getParent();
    if (parent == null) {
      return;
    }

    if (!Files.isDirectory(parent)) {
      Files.createDirectories(parent, attrs);
      if (!Files.isDirectory(parent)) {
        throw new IOException("Unable to create parent directories of " + path);
      }
    }
  }

  /**
   * Returns the <a href="http:
   * the file at the given path, or the empty string if the file has no extension. The result does
   * not include the '{@code .}'.
   *
   * <p><b>Note:</b> This method simply returns everything after the last '{@code .}' in the file's
   * name as determined by {@link Path#getFileName}. It does not account for any filesystem-specific
   * behavior that the {@link Path} API does not already account for. For example, on NTFS it will
   * report {@code "txt"} as the extension for the filename {@code "foo.exe:.txt"} even though NTFS
   * will drop the {@code ":.txt"} part of the name when the file is actually created on the
   * filesystem due to NTFS's <a href="https:
   */
  public static String getFileExtension(Path path) {
    Path name = path.getFileName();

    if (name == null) {
      return "";
    }

    String fileName = name.toString();
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
  }

  /**
   * Returns the file name without its <a
   * href="http:
   * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
   */
  public static String getNameWithoutExtension(Path path) {
    Path name = path.getFileName();

    if (name == null) {
      return "";
    }

    String fileName = name.toString();
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
  }

  /**
   * Deletes the file or directory at the given {@code path} recursively. Deletes symbolic links,
   * not their targets (subject to the caveat below).
   *
   * <p>If an I/O exception occurs attempting to read, open or delete any file under the given
   * directory, this method skips that file and continues. All such exceptions are collected and,
   * after attempting to delete all files, an {@code IOException} is thrown containing those
   * exceptions as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
   *
   * <h2>Warning: Security of recursive deletes</h2>
   *
   * <p>On a file system that supports symbolic links and does <i>not</i> support {@link
   * SecureDirectoryStream}, it is possible for a recursive delete to delete files and directories
   * that are <i>outside</i> the directory being deleted. This can happen if, after checking that a
   * file is a directory (and not a symbolic link), that directory is replaced by a symbolic link to
   * an outside directory before the call that opens the directory to read its entries.
   *
   * <p>By default, this method throws {@link InsecureRecursiveDeleteException} if it can't
   * guarantee the security of recursive deletes. If you wish to allow the recursive deletes anyway,
   * pass {@link RecursiveDeleteOption#ALLOW_INSECURE} to this method to override that behavior.
   *
   * @throws NoSuchFileException if {@code path} does not exist <i>(optional specific exception)</i>
   * @throws InsecureRecursiveDeleteException if the security of recursive deletes can't be
   *     guaranteed for the file system and {@link RecursiveDeleteOption#ALLOW_INSECURE} was not
   *     specified
   * @throws IOException if {@code path} or any file in the subtree rooted at it can't be deleted
   *     for any reason
   */
  public static void deleteRecursively(Path path, RecursiveDeleteOption... options)
      throws IOException {
    Path parentPath = getParentPath(path);
    if (parentPath == null) {
      throw new FileSystemException(path.toString(), null, "can't delete recursively");
    }

    Collection<IOException> exceptions = null; 
    try {
      boolean sdsSupported = false;
      try (DirectoryStream<Path> parent = Files.newDirectoryStream(parentPath)) {
        if (parent instanceof SecureDirectoryStream) {
          sdsSupported = true;
          exceptions =
              deleteRecursivelySecure(
                  (SecureDirectoryStream<Path>) parent,
                  /*
                   * requireNonNull is safe because paths have file names when they have parents,
                   * and we checked for a parent at the beginning of the method.
                   */
                  requireNonNull(path.getFileName()));
        }
      }

      if (!sdsSupported) {
        checkAllowsInsecure(path, options);
        exceptions = deleteRecursivelyInsecure(path);
      }
    } catch (IOException e) {
      if (exceptions == null) {
        throw e;
      } else {
        exceptions.add(e);
      }
    }

    if (exceptions != null) {
      throwDeleteFailed(path, exceptions);
    }
  }

  /**
   * Deletes all files within the directory at the given {@code path} {@linkplain #deleteRecursively
   * recursively}. Does not delete the directory itself. Deletes symbolic links, not their targets
   * (subject to the caveat below). If {@code path} itself is a symbolic link to a directory, that
   * link is followed and the contents of the directory it targets are deleted.
   *
   * <p>If an I/O exception occurs attempting to read, open or delete any file under the given
   * directory, this method skips that file and continues. All such exceptions are collected and,
   * after attempting to delete all files, an {@code IOException} is thrown containing those
   * exceptions as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
   *
   * <h2>Warning: Security of recursive deletes</h2>
   *
   * <p>On a file system that supports symbolic links and does <i>not</i> support {@link
   * SecureDirectoryStream}, it is possible for a recursive delete to delete files and directories
   * that are <i>outside</i> the directory being deleted. This can happen if, after checking that a
   * file is a directory (and not a symbolic link), that directory is replaced by a symbolic link to
   * an outside directory before the call that opens the directory to read its entries.
   *
   * <p>By default, this method throws {@link InsecureRecursiveDeleteException} if it can't
   * guarantee the security of recursive deletes. If you wish to allow the recursive deletes anyway,
   * pass {@link RecursiveDeleteOption#ALLOW_INSECURE} to this method to override that behavior.
   *
   * @throws NoSuchFileException if {@code path} does not exist <i>(optional specific exception)</i>
   * @throws NotDirectoryException if the file at {@code path} is not a directory <i>(optional
   *     specific exception)</i>
   * @throws InsecureRecursiveDeleteException if the security of recursive deletes can't be
   *     guaranteed for the file system and {@link RecursiveDeleteOption#ALLOW_INSECURE} was not
   *     specified
   * @throws IOException if one or more files can't be deleted for any reason
   */
  public static void deleteDirectoryContents(Path path, RecursiveDeleteOption... options)
      throws IOException {
    Collection<IOException> exceptions = null; 
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      if (stream instanceof SecureDirectoryStream) {
        SecureDirectoryStream<Path> sds = (SecureDirectoryStream<Path>) stream;
        exceptions = deleteDirectoryContentsSecure(sds);
      } else {
        checkAllowsInsecure(path, options);
        exceptions = deleteDirectoryContentsInsecure(stream);
      }
    } catch (IOException e) {
      if (exceptions == null) {
        throw e;
      } else {
        exceptions.add(e);
      }
    }

    if (exceptions != null) {
      throwDeleteFailed(path, exceptions);
    }
  }

  /**
   * Secure recursive delete using {@code SecureDirectoryStream}. Returns a collection of exceptions
   * that occurred or null if no exceptions were thrown.
   */
  @CheckForNull
  private static Collection<IOException> deleteRecursivelySecure(
      SecureDirectoryStream<Path> dir, Path path) {
    Collection<IOException> exceptions = null;
    try {
      if (isDirectory(dir, path, NOFOLLOW_LINKS)) {
        try (SecureDirectoryStream<Path> childDir = dir.newDirectoryStream(path, NOFOLLOW_LINKS)) {
          exceptions = deleteDirectoryContentsSecure(childDir);
        }

        if (exceptions == null) {
          dir.deleteDirectory(path);
        }
      } else {
        dir.deleteFile(path);
      }

      return exceptions;
    } catch (IOException e) {
      return addException(exceptions, e);
    }
  }

  /**
   * Secure method for deleting the contents of a directory using {@code SecureDirectoryStream}.
   * Returns a collection of exceptions that occurred or null if no exceptions were thrown.
   */
  @CheckForNull
  private static Collection<IOException> deleteDirectoryContentsSecure(
      SecureDirectoryStream<Path> dir) {
    Collection<IOException> exceptions = null;
    try {
      for (Path path : dir) {
        exceptions = concat(exceptions, deleteRecursivelySecure(dir, path.getFileName()));
      }

      return exceptions;
    } catch (DirectoryIteratorException e) {
      return addException(exceptions, e.getCause());
    }
  }

  /**
   * Insecure recursive delete for file systems that don't support {@code SecureDirectoryStream}.
   * Returns a collection of exceptions that occurred or null if no exceptions were thrown.
   */
  @CheckForNull
  private static Collection<IOException> deleteRecursivelyInsecure(Path path) {
    Collection<IOException> exceptions = null;
    try {
      if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
          exceptions = deleteDirectoryContentsInsecure(stream);
        }
      }

      if (exceptions == null) {
        Files.delete(path);
      }

      return exceptions;
    } catch (IOException e) {
      return addException(exceptions, e);
    }
  }

  /**
   * Simple, insecure method for deleting the contents of a directory for file systems that don't
   * support {@code SecureDirectoryStream}. Returns a collection of exceptions that occurred or null
   * if no exceptions were thrown.
   */
  @CheckForNull
  private static Collection<IOException> deleteDirectoryContentsInsecure(
      DirectoryStream<Path> dir) {
    Collection<IOException> exceptions = null;
    try {
      for (Path entry : dir) {
        exceptions = concat(exceptions, deleteRecursivelyInsecure(entry));
      }

      return exceptions;
    } catch (DirectoryIteratorException e) {
      return addException(exceptions, e.getCause());
    }
  }

  /**
   * Returns a path to the parent directory of the given path. If the path actually has a parent
   * path, this is simple. Otherwise, we need to do some trickier things. Returns null if the path
   * is a root or is the empty path.
   */
  @CheckForNull
  private static Path getParentPath(Path path) {
    Path parent = path.getParent();

    if (parent != null) {
      return parent;
    }

    if (path.getNameCount() == 0) {
      return null;
    } else {
      return path.getFileSystem().getPath(".");
    }
  }

  /** Checks that the given options allow an insecure delete, throwing an exception if not. */
  private static void checkAllowsInsecure(Path path, RecursiveDeleteOption[] options)
      throws InsecureRecursiveDeleteException {
    if (!Arrays.asList(options).contains(RecursiveDeleteOption.ALLOW_INSECURE)) {
      throw new InsecureRecursiveDeleteException(path.toString());
    }
  }

  /**
   * Adds the given exception to the given collection, creating the collection if it's null. Returns
   * the collection.
   */
  private static Collection<IOException> addException(
      @CheckForNull Collection<IOException> exceptions, IOException e) {
    if (exceptions == null) {
      exceptions = new ArrayList<>(); 
    }
    exceptions.add(e);
    return exceptions;
  }

  /**
   * Concatenates the contents of the two given collections of exceptions. If either collection is
   * null, the other collection is returned. Otherwise, the elements of {@code other} are added to
   * {@code exceptions} and {@code exceptions} is returned.
   */
  @CheckForNull
  private static Collection<IOException> concat(
      @CheckForNull Collection<IOException> exceptions,
      @CheckForNull Collection<IOException> other) {
    if (exceptions == null) {
      return other;
    } else if (other != null) {
      exceptions.addAll(other);
    }
    return exceptions;
  }

  /**
   * Throws an exception indicating that one or more files couldn't be deleted when deleting {@code
   * path} or its contents.
   *
   * <p>If there is only one exception in the collection, and it is a {@link NoSuchFileException}
   * thrown because {@code path} itself didn't exist, then throws that exception. Otherwise, the
   * thrown exception contains all the exceptions in the given collection as suppressed exceptions.
   */
  private static void throwDeleteFailed(Path path, Collection<IOException> exceptions)
      throws FileSystemException {
    NoSuchFileException pathNotFound = pathNotFound(path, exceptions);
    if (pathNotFound != null) {
      throw pathNotFound;
    }
    FileSystemException deleteFailed =
        new FileSystemException(
            path.toString(),
            null,
            "failed to delete one or more files; see suppressed exceptions for details");
    for (IOException e : exceptions) {
      deleteFailed.addSuppressed(e);
    }
    throw deleteFailed;
  }

  @CheckForNull
  private static NoSuchFileException pathNotFound(Path path, Collection<IOException> exceptions) {
    if (exceptions.size() != 1) {
      return null;
    }
    IOException exception = getOnlyElement(exceptions);
    if (!(exception instanceof NoSuchFileException)) {
      return null;
    }
    NoSuchFileException noSuchFileException = (NoSuchFileException) exception;
    String exceptionFile = noSuchFileException.getFile();
    if (exceptionFile == null) {
      /*
       * It's not clear whether this happens in practice, especially with the filesystem
       * implementations that are built into java.nio.
       */
      return null;
    }
    Path parentPath = getParentPath(path);
    if (parentPath == null) {
      /*
       * This is probably impossible:
       *
       * - In deleteRecursively, we require the path argument to have a parent.
       *
       * - In deleteDirectoryContents, the path argument may have no parent. Fortunately, all the
       *   *other* paths we process will be descendants of that. That leaves only the original path
       *   argument for us to consider. And the only place we call pathNotFound is from
       *   throwDeleteFailed, and the other place that we call throwDeleteFailed inside
       *   deleteDirectoryContents is when an exception is thrown during the recursive steps. Any
       *   failure during the initial lookup of the path argument itself is rethrown directly. So
       *   any exception that we're seeing here is from a descendant, which naturally has a parent.
       *   I think.
       *
       * Still, if this can happen somehow (a weird filesystem implementation that lets callers
       * change its working directly concurrently with a call to deleteDirectoryContents?), it makes
       * more sense for us to fall back to a generic FileSystemException (by returning null here)
       * than to dereference parentPath and end up producing NullPointerException.
       */
      return null;
    }
    Path pathResolvedFromParent = parentPath.resolve(requireNonNull(path.getFileName()));
    if (exceptionFile.equals(pathResolvedFromParent.toString())) {
      return noSuchFileException;
    }
    return null;
  }
}
