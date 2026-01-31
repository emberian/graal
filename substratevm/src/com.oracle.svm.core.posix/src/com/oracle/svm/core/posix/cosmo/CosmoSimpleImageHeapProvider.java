/*
 * Copyright (c) 2025 Elide Technologies. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package com.oracle.svm.core.posix.cosmo;

import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.c.function.CEntryPointErrors;
import com.oracle.svm.core.headers.LibC;
import com.oracle.svm.core.heap.Heap;
import com.oracle.svm.core.os.AbstractCopyingImageHeapProvider;
import com.oracle.svm.core.os.VirtualMemoryProvider;
import com.oracle.svm.core.util.UnsignedUtils;
import org.graalvm.word.impl.Word;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.Pointer;
import org.graalvm.word.UnsignedWord;

import static com.oracle.svm.core.Isolates.IMAGE_HEAP_BEGIN;
import static com.oracle.svm.core.Isolates.IMAGE_HEAP_END;
import static com.oracle.svm.core.Isolates.IMAGE_HEAP_WRITABLE_BEGIN;
import static com.oracle.svm.core.Isolates.IMAGE_HEAP_WRITABLE_END;
import static com.oracle.svm.core.util.PointerUtils.roundUp;
import static org.graalvm.nativeimage.c.function.CFunction.Transition.NO_TRANSITION;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.CContext.Directives;
import java.util.Collections;
import java.util.List;

@CContext(CosmoSimpleImageHeapProvider.CosmoHeapDirectives.class)
public class CosmoSimpleImageHeapProvider extends AbstractCopyingImageHeapProvider {

    /**
     * CContext directives to link the debug_heap library containing PC-relative
     * address resolution functions for cosmo binaries.
     */
    public static class CosmoHeapDirectives implements Directives {
        @Override
        public List<String> getLibraries() {
            return Collections.singletonList("debug_heap");
        }

        @Override
        public boolean isInConfiguration() {
            return true;
        }
    }

    /**
     * Get actual runtime address of __svm_heap_begin using PC-relative addressing.
     * This is necessary because static ELF binaries may be loaded at a different
     * address than their link-time address when run via the APE loader on macOS.
     */
    @CFunction(value = "cosmo_get_heap_begin", transition = NO_TRANSITION)
    private static native Word cosmoGetHeapBegin();

    @CFunction(value = "cosmo_get_heap_end", transition = NO_TRANSITION)
    private static native Word cosmoGetHeapEnd();

    @CFunction(value = "cosmo_get_heap_writable_begin", transition = NO_TRANSITION)
    private static native Word cosmoGetHeapWritableBegin();

    @CFunction(value = "cosmo_get_heap_writable_end", transition = NO_TRANSITION)
    private static native Word cosmoGetHeapWritableEnd();

    /**
     * Debug function to print address comparison (GOT vs PC-relative).
     * This helps verify if the binary was loaded at a different address.
     */
    @CFunction(value = "cosmo_debug_all_heap_addresses", transition = NO_TRANSITION)
    private static native void cosmoDebugAllHeapAddresses(Word gotBegin, Word gotEnd,
                                                           Word gotWritableBegin, Word gotWritableEnd);

    @Override
    @Uninterruptible(reason = "Called during isolate initialization.")
    protected int copyMemory(Pointer loadedImageHeap, UnsignedWord imageHeapSize, Pointer newImageHeap) {
        LibC.memcpy(newImageHeap, loadedImageHeap, imageHeapSize);
        return CEntryPointErrors.NO_ERROR;
    }

    @Override
    @Uninterruptible(reason = "Called during isolate initialization.")
    protected int commitAndCopyMemory(Pointer loadedImageHeap, UnsignedWord imageHeapSize, Pointer newImageHeap) {
        Pointer actualNewImageHeap = VirtualMemoryProvider.get().commit(newImageHeap, imageHeapSize, VirtualMemoryProvider.Access.READ | VirtualMemoryProvider.Access.WRITE);
        if (actualNewImageHeap.isNull() || actualNewImageHeap.notEqual(newImageHeap)) {
            return CEntryPointErrors.RESERVE_ADDRESS_SPACE_FAILED;
        }
        return copyMemory(loadedImageHeap, imageHeapSize, newImageHeap);
    }

    @Override
    @Uninterruptible(reason = "Called during isolate initialization.")
    public int initialize(Pointer reservedAddressSpace, UnsignedWord reservedSize, WordPointer basePointer, WordPointer endPointer) {
        Pointer selfReservedMemory = Word.nullPointer();
        UnsignedWord requiredSize = getTotalRequiredAddressSpaceSize();
        if (reservedAddressSpace.isNull()) {
            UnsignedWord alignment = Word.unsigned(Heap.getHeap().getImageHeapAlignment());
            selfReservedMemory = VirtualMemoryProvider.get().reserve(requiredSize, alignment, false);
            if (selfReservedMemory.isNull()) {
                return CEntryPointErrors.RESERVE_ADDRESS_SPACE_FAILED;
            }
        } else if (reservedSize.belowThan(requiredSize)) {
            return CEntryPointErrors.INSUFFICIENT_ADDRESS_SPACE;
        }

        Pointer heapBase;
        Pointer selfReservedHeapBase;
        heapBase = selfReservedMemory.isNonNull() ? selfReservedMemory : reservedAddressSpace;
        selfReservedHeapBase = selfReservedMemory;

        // Get actual runtime addresses using PC-relative addressing.
        // This is critical for cosmo binaries that may be loaded at different addresses
        // than their link-time addresses when run via the APE loader on macOS.
        Word actualHeapBegin = cosmoGetHeapBegin();
        Word actualHeapEnd = cosmoGetHeapEnd();
        Word actualWritableBegin = cosmoGetHeapWritableBegin();
        Word actualWritableEnd = cosmoGetHeapWritableEnd();

        // Debug: print address comparison to verify if relocation occurred
        cosmoDebugAllHeapAddresses(IMAGE_HEAP_BEGIN.get(), IMAGE_HEAP_END.get(),
                                   IMAGE_HEAP_WRITABLE_BEGIN.get(), IMAGE_HEAP_WRITABLE_END.get());

        // Copy the memory to the reserved address space.
        // Use PC-relative addresses for the source, not the link-time addresses from CGlobalData.
        UnsignedWord imageHeapSizeInFile = getImageHeapSizeInFile(actualHeapBegin, actualHeapEnd);
        Pointer imageHeap = getImageHeapBegin(heapBase);
        int result = commitAndCopyMemory((Pointer) actualHeapBegin, imageHeapSizeInFile, imageHeap);
        if (result != CEntryPointErrors.NO_ERROR) {
            freeImageHeap(selfReservedHeapBase);
            return result;
        }

        // Protect the read-only parts at the start of the image heap.
        // Use actual PC-relative addresses for the offset calculations.
        UnsignedWord pageSize = VirtualMemoryProvider.get().getGranularity();
        UnsignedWord writableBeginPageOffset = UnsignedUtils.roundDown(actualWritableBegin.subtract(actualHeapBegin), pageSize);
        if (writableBeginPageOffset.aboveThan(0)) {
            if (VirtualMemoryProvider.get().protect(imageHeap, writableBeginPageOffset, VirtualMemoryProvider.Access.READ) != 0) {
                freeImageHeap(selfReservedHeapBase);
                return CEntryPointErrors.PROTECT_HEAP_FAILED;
            }
        }

        // Protect the read-only parts at the end of the image heap.
        UnsignedWord writableEndPageOffset = UnsignedUtils.roundUp(actualWritableEnd.subtract(actualHeapBegin), pageSize);
        if (writableEndPageOffset.belowThan(imageHeapSizeInFile)) {
            Pointer afterWritableBoundary = imageHeap.add(writableEndPageOffset);
            UnsignedWord afterWritableSize = imageHeapSizeInFile.subtract(writableEndPageOffset);
            if (VirtualMemoryProvider.get().protect(afterWritableBoundary, afterWritableSize, VirtualMemoryProvider.Access.READ) != 0) {
                freeImageHeap(selfReservedHeapBase);
                return CEntryPointErrors.PROTECT_HEAP_FAILED;
            }
        }

        // Update the heap base and end pointers.
        basePointer.write(heapBase);
        if (endPointer.isNonNull()) {
            endPointer.write(roundUp(imageHeap.add(imageHeapSizeInFile), pageSize));
        }
        return CEntryPointErrors.NO_ERROR;
    }
}
