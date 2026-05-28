/*=============================================================================
	UnName.h: Unreal global name types.
	Copyright 1997-1999 Epic Games, Inc. All Rights Reserved.

	Revision history:
		* Created by Tim Sweeney
=============================================================================*/

/*----------------------------------------------------------------------------
	Definitions.
----------------------------------------------------------------------------*/
#include <android/log.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "Unreal", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "Unreal", __VA_ARGS__))
// Maximum size of name.
enum {NAME_SIZE	= 64};

// Name index.
typedef INT NAME_INDEX;

// Enumeration for finding name.
enum EFindName
{
	FNAME_Find,			// Find a name; return 0 if it doesn't exist.
	FNAME_Add,			// Find a name or add it if it doesn't exist.
	FNAME_Intrinsic,	// Find a name or add it intrinsically if it doesn't exist.
};

/*----------------------------------------------------------------------------
	FNameEntry.
----------------------------------------------------------------------------*/

//
// A global name, as stored in the global name table.
//
struct FNameEntry
{
	// Variables.
	NAME_INDEX	Index;				// Index of name in hash.
	DWORD		Flags;				// RF_TagImp, RF_TagExp, RF_Native.
	FNameEntry*	HashNext;			// Pointer to the next entry in this hash bin's linked list.

	// The name string.
	TCHAR		Name[NAME_SIZE];	// Name, variable-sized.

	// Functions.
	CORE_API friend FArchive& operator<<( FArchive& Ar, FNameEntry& E );
	CORE_API friend FNameEntry* AllocateNameEntry( const TCHAR* Name, DWORD Index, DWORD Flags, FNameEntry* HashNext );
};
template <> struct TTypeInfo<FNameEntry*> : public TTypeInfoBase<FNameEntry*>
{
	static UBOOL NeedsDestructor() {return 0;}
};

/*----------------------------------------------------------------------------
	FName.
----------------------------------------------------------------------------*/

//
// Public name, available to the world.  Names are stored as WORD indices
// into the name table and every name in Unreal is stored once
// and only once in that table.  Names are case-insensitive.
//
#define checkName checkSlow
class CORE_API FName 
{
public:
	// Accessors.
	const TCHAR* operator*() const
	{
		// if (Index >= 0 && Index < Names.Num())
		// {
		// 	LOGI("Entry=%p", Names(Index));

		// 	if (Names(Index))
		// 	{
		// 		LOGI("Name=%s",
		// 			Names(Index)->Name
		// 				? Names(Index)->Name
		// 				: "NULL");
		// 	}
		// }
		if (!Initialized || Names.Num() == 0)
    		return TEXT("FNameBooting");
		
		checkName(Index < Names.Num());
		checkName(Names(Index));
		return Names(Index)->Name;
	}
	NAME_INDEX GetIndex() const
	{
		checkName(Index < Names.Num());
		checkName(Names(Index));
		return Index;
	}
	DWORD GetFlags() const
	{
		checkName(Index < Names.Num());
		checkName(Names(Index));
		return Names(Index)->Flags;
	}
	void SetFlags( DWORD Set ) const
	{
		checkName(Index < Names.Num());
		checkName(Names(Index));
		Names(Index)->Flags |= Set;
	}
	void ClearFlags( DWORD Clear ) const
	{
		checkName(Index < Names.Num());
		checkName(Names(Index));
		Names(Index)->Flags &= ~Clear;
	}
	UBOOL operator==( const FName& Other ) const
	{
		return Index==Other.Index;
	}
	UBOOL operator!=( const FName& Other ) const
	{
		return Index!=Other.Index;
	}
	UBOOL IsValid()
	{
		return Index>=0 && Index<Names.Num() && Names(Index)!=NULL;
	}

	// Constructors.
	FName( enum EName N )
	: Index( N )
	{}
	FName()
	{}
	FName( const TCHAR* Name, EFindName FindType=FNAME_Add );

	// Name subsystem.
	static void StaticInit();
	static void StaticExit();
	static void DeleteEntry( int i );
	static void DisplayHash( class FOutputDevice& Ar );
	static void Hardcode( FNameEntry* AutoName );

	// Name subsystem accessors.
	// Name subsystem accessors.
	static const TCHAR* SafeString( EName Index )
	{
		if (GIsRequestingExit)
			return TEXT("Shutdown");

		if (!Initialized || Names.Num() == 0)
			return TEXT("Uninitialized");

		if ((INT)Index < 0 || (INT)Index >= Names.Num())
		{
			LOGE("SafeString invalid index=%d", (int)Index);
			return TEXT("BadIndex");
		}

		FNameEntry* Entry = Names(Index);

		if (!Entry)
		{
			LOGE("SafeString NULL entry");
			return TEXT("NullEntry");
		}

		if (!Entry->Name)
		{
			LOGE("SafeString NULL name");
			return TEXT("NullName");
		}

		return Entry->Name;
	}
	static UBOOL SafeSuppressed( EName Index )
	{
		if (!Initialized)
        	return 0;

		INT Idx = (INT)Index;

		if (Idx < 0 || Idx >= Names.Num())
		{
			LOGE("Invalid index=%d Num=%d",
				Idx,
				Names.Num());
			return 0;
		}

		FNameEntry* Entry = Names(Idx);
		if (!Entry)
		{
			LOGE("NULL name entry at %d", Idx);
			return 0;
		}

		return (Entry->Flags & 0x00001000);
	}
	static int GetMaxNames()
	{
		return Names.Num();
	}
	static FNameEntry* GetEntry( int i )
	{
		return Names(i);
	}
	static UBOOL GetInitialized()
	{
		return Initialized;
	}

private:
	// Name index.
	NAME_INDEX Index;

	// Static subsystem variables.
	static TArray<FNameEntry*>	Names;			 // Table of all names.
	static TArray<INT>          Available;       // Indices of available names.
	static FNameEntry*			NameHash[4096];  // Hashed names.
	static UBOOL				Initialized;	 // Subsystem initialized.
};
inline DWORD GetTypeHash( const FName N )
{
	return N.GetIndex();
}

/*----------------------------------------------------------------------------
	The End.
----------------------------------------------------------------------------*/
