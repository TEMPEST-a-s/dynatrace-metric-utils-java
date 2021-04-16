/**
 * Copyright 2021 Dynatrace LLC
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.metric.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class OneAgentMetadataEnricherTest {
  @Test
  public void validMetrics() {
    ArrayList<Dimension> entries =
        new ArrayList<>(
            OneAgentMetadataEnricher.parseOneAgentMetadata(
                Arrays.asList("prop.a=value.a", "prop.b=value.b")));

    assertEquals("prop.a", entries.get(0).getKey());
    assertEquals("value.a", entries.get(0).getValue());
    assertEquals("prop.b", entries.get(1).getKey());
    assertEquals("value.b", entries.get(1).getValue());
  }

  @Test
  public void invalidMetrics() {
    assertTrue(
        OneAgentMetadataEnricher.parseOneAgentMetadata(
                Collections.singletonList("=0x5c14d9a68d569861"))
            .isEmpty());
    assertTrue(
        OneAgentMetadataEnricher.parseOneAgentMetadata(Collections.singletonList("key_no_value="))
            .isEmpty());
    assertTrue(
        OneAgentMetadataEnricher.parseOneAgentMetadata(Collections.singletonList("==============="))
            .isEmpty());
    assertTrue(
        OneAgentMetadataEnricher.parseOneAgentMetadata(Collections.singletonList("")).isEmpty());
    assertTrue(
        OneAgentMetadataEnricher.parseOneAgentMetadata(Collections.singletonList("=")).isEmpty());
    assertTrue(OneAgentMetadataEnricher.parseOneAgentMetadata(Collections.emptyList()).isEmpty());
  }

  @Test
  public void testGetIndirectionFileContentValid() throws IOException {
    String expected =
        "dt_metadata_e617c525669e072eebe3d0f08212e8f2_private_target_file_specifier.properties";
    // "mock" the contents of dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties
    StringReader reader = new StringReader(expected);
    String result = OneAgentMetadataEnricher.getMetadataFileName(reader);
    assertEquals(expected, result);
  }

  @Test
  public void testGetIndirectionFilePassNull() {
    assertThrows(IOException.class, () -> OneAgentMetadataEnricher.getMetadataFileName(null));
  }

  @Test
  public void testGetIndirectionFileContentEmptyContent() throws IOException {
    StringReader reader = new StringReader("");
    assertNull(OneAgentMetadataEnricher.getMetadataFileName(reader));
  }

  @Test
  public void testGetOneAgentMetadataFileContentValid() throws IOException {
    List<String> expected = new ArrayList<>();
    expected.add("key1=value1");
    expected.add("key2=value2");
    expected.add("key3=value3");

    StringReader reader = new StringReader(String.join("\n", expected));
    List<String> result = OneAgentMetadataEnricher.getOneAgentMetadataFileContent(reader);
    assertEquals(expected, result);
    assertNotSame(expected, result);
  }

  @Test
  public void testGetOneAgentMetadataFileContentInvalid() throws IOException {
    List<String> inputs = Arrays.asList("=0", "", "a=", "\t\t", "=====", "    ", "   test   ");
    List<String> expected = Arrays.asList("=0", "", "a=", "", "=====", "", "test");

    StringReader reader = new StringReader(String.join("\n", inputs));
    List<String> result = OneAgentMetadataEnricher.getOneAgentMetadataFileContent(reader);
    assertEquals(expected, result);
    assertNotSame(expected, result);
  }

  @Test
  public void testGetOneAgentMetadataFileContentEmptyFile() throws IOException {
    List<String> expected = new ArrayList<>();

    List<String> result =
        OneAgentMetadataEnricher.getOneAgentMetadataFileContent(new StringReader(""));
    assertEquals(expected, result);
    assertNotSame(expected, result);
  }

  @Test
  public void testGetOneAgentMetadataFileContentPassNull() {
    assertThrows(
        IOException.class, () -> OneAgentMetadataEnricher.getOneAgentMetadataFileContent(null));
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_Valid() {
    List<String> expected = Arrays.asList("key1=value1", "key2=value2", "key3=value3");
    List<String> results =
        OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
            "src/test/resources/indirection.properties");
    assertEquals(expected, results);
    assertNotSame(expected, results);
  }

  private String generateNonExistentFilename() {
    File f;
    Random r = new Random();
    // generate random filenames until we find one that does not exist:
    do {
      byte[] array = new byte[7];
      r.nextBytes(array);
      String filename =
          "src/test/resources/" + new String(array, StandardCharsets.UTF_8) + ".properties";

      f = new File(filename);
    } while (f.exists());
    return f.getAbsolutePath();
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_IndirectionFileDoesNotExist() {
    String filename = generateNonExistentFilename();

    List<String> result = OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(filename);
    assertEquals(Collections.<String>emptyList(), result);
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_IndirectionFileReturnsNull() {
    try (MockedStatic<OneAgentMetadataEnricher> mockEnricher =
        Mockito.mockStatic(OneAgentMetadataEnricher.class)) {
      mockEnricher
          .when(() -> OneAgentMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn(null);
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties");
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_IndirectionFileReturnsEmpty() {
    try (MockedStatic<OneAgentMetadataEnricher> mockEnricher =
        Mockito.mockStatic(OneAgentMetadataEnricher.class)) {
      // ignore the return value of the testfile and mock the return value of the
      // getIndirectionFileName call:
      mockEnricher
          .when(() -> OneAgentMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("");
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties");
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_IndirectionFileThrows() {
    try (MockedStatic<OneAgentMetadataEnricher> mockEnricher =
        Mockito.mockStatic(OneAgentMetadataEnricher.class)) {
      // ignore the return value of the testfile and mock the return value of the
      // getIndirectionFileName call:
      mockEnricher
          .when(() -> OneAgentMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenThrow(new IOException("test exception"));
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties");
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_MetadataFileDoesNotExist() {
    String metadataFilename = generateNonExistentFilename();
    try (MockedStatic<OneAgentMetadataEnricher> mockEnricher =
        Mockito.mockStatic(OneAgentMetadataEnricher.class)) {
      mockEnricher
          .when(() -> OneAgentMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn(metadataFilename);
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties");
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_MetadataFileReadThrows() {
    try (MockedStatic<OneAgentMetadataEnricher> mockEnricher =
        Mockito.mockStatic(OneAgentMetadataEnricher.class)) {
      mockEnricher
          .when(() -> OneAgentMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("src/test/resources/mock_target.properties");
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getOneAgentMetadataFileContent(
                      Mockito.any(FileReader.class)))
          .thenThrow(new IOException("test exception"));
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties");
      assertEquals(Collections.<String>emptyList(), result);
    }
  }

  @Test
  public void testGetMetadataFileContentWithRedirection_EmptyMetadataFile() {
    try (MockedStatic<OneAgentMetadataEnricher> mockEnricher =
        Mockito.mockStatic(OneAgentMetadataEnricher.class)) {
      mockEnricher
          .when(() -> OneAgentMetadataEnricher.getMetadataFileName(Mockito.any(FileReader.class)))
          .thenReturn("src/test/resources/mock_target.properties");
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getOneAgentMetadataFileContent(
                      Mockito.any(FileReader.class)))
          .thenReturn(Collections.emptyList());
      mockEnricher
          .when(
              () ->
                  OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
                      Mockito.anyString()))
          .thenCallRealMethod();

      List<String> result =
          OneAgentMetadataEnricher.getMetadataFileContentWithRedirection(
              "src/test/resources/mock_target.properties");
      assertEquals(Collections.<String>emptyList(), result);
    }
  }
}