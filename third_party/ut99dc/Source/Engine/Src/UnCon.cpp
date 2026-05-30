/*=============================================================================
	UnCon.cpp: Implementation of UConsole class
	Copyright 1997-1999 Epic Games, Inc. All Rights Reserved.
=============================================================================*/

#include "EnginePrivate.h"
#include "UnRender.h"

/*------------------------------------------------------------------------------
	UConsole object implementation.
------------------------------------------------------------------------------*/

IMPLEMENT_CLASS(UConsole);

static void ResizeClassDefaults( UClass* Class, INT NewSize )
{
	if( Class && Class->Defaults.Num() )
	{
		if( Class->Defaults.Num() < NewSize )
			Class->Defaults.AddZeroed( NewSize - Class->Defaults.Num() );
		else if( Class->Defaults.Num() > NewSize )
			Class->Defaults.Remove( NewSize, Class->Defaults.Num() - NewSize );
	}
}

static INT GetConsolePropertyOffset( UClass* Class, const TCHAR* Name )
{
	for( TFieldIterator<UProperty> It(Class); It; ++It )
		if( It->GetOuter()==Class && appStricmp(It->GetName(),Name)==0 )
			return It->Offset;
	return INDEX_NONE;
}

static void SetConsolePropertyOffsetImpl( UClass* Class, const TCHAR* Name, INT Offset, TArray<BYTE>* RebuiltDefaults )
{
	for( TFieldIterator<UProperty> It(Class); It; ++It )
	{
		if( It->GetOuter()==Class && appStricmp(It->GetName(),Name)==0 )
		{
			if( RebuiltDefaults && Class->Defaults.Num() && It->Offset>=0 && It->Offset+It->GetSize()<=Class->Defaults.Num() && Offset>=0 && Offset+It->GetSize()<=RebuiltDefaults->Num() )
				It->CopyCompleteValue( &(*RebuiltDefaults)(Offset), &Class->Defaults(It->Offset) );
			if( It->Offset != Offset )
			{
				debugf( NAME_Warning, TEXT("UT99_ANDROID_V172_CONSOLE_OFFSET_FIX property=%s script=%i native=%i"), Name, It->Offset, Offset );
				It->Offset = Offset;
			}
			break;
		}
	}
}

#define SetConsolePropertyOffset(Class,Name,Offset) SetConsolePropertyOffsetImpl(Class,Name,Offset,RebuiltDefaultsPtr)

void UConsole::FixupNativeClassSize( UClass* ConsoleClass )
{
	guard(UConsole::FixupNativeClassSize);
#if defined(PLATFORM_64BIT)
	UClass* BaseClass = UConsole::StaticClass();
	const INT OldBaseSize = BaseClass->GetPropertiesSize();
	const INT NativeBaseSize = sizeof(UConsole);
	const INT Delta = NativeBaseSize - OldBaseSize;
	TArray<BYTE> RebuiltDefaults;
	TArray<BYTE>* RebuiltDefaultsPtr = NULL;

	if( Delta )
	{
		debugf
		(
			NAME_Warning,
			TEXT("UT99_ANDROID_V171_CONSOLE_SIZE_FIX base=%s script=%i native=%i delta=%i"),
			BaseClass->GetFullName(),
			OldBaseSize,
			NativeBaseSize,
			Delta
		);
		BaseClass->SetPropertiesSize( NativeBaseSize );
		ResizeClassDefaults( BaseClass, NativeBaseSize );
	}
	if( BaseClass->Defaults.Num() )
	{
		RebuiltDefaults.AddZeroed( NativeBaseSize );
		appMemcpy( &RebuiltDefaults(0), &BaseClass->Defaults(0), Min<INT>( sizeof(UObject), BaseClass->Defaults.Num() ) );
		RebuiltDefaultsPtr = &RebuiltDefaults;
	}

	SetConsolePropertyOffset( BaseClass, TEXT("Viewport"), STRUCT_OFFSET(UConsole,Viewport) );
	SetConsolePropertyOffset( BaseClass, TEXT("HistoryTop"), STRUCT_OFFSET(UConsole,HistoryTop) );
	SetConsolePropertyOffset( BaseClass, TEXT("HistoryBot"), STRUCT_OFFSET(UConsole,HistoryBot) );
	SetConsolePropertyOffset( BaseClass, TEXT("HistoryCur"), STRUCT_OFFSET(UConsole,HistoryCur) );
	SetConsolePropertyOffset( BaseClass, TEXT("TypedStr"), STRUCT_OFFSET(UConsole,TypedStr) );
	SetConsolePropertyOffset( BaseClass, TEXT("History"), STRUCT_OFFSET(UConsole,History) );
	SetConsolePropertyOffset( BaseClass, TEXT("Scrollback"), STRUCT_OFFSET(UConsole,Scrollback) );
	SetConsolePropertyOffset( BaseClass, TEXT("NumLines"), STRUCT_OFFSET(UConsole,numLines) );
	SetConsolePropertyOffset( BaseClass, TEXT("TopLine"), STRUCT_OFFSET(UConsole,TopLine) );
	SetConsolePropertyOffset( BaseClass, TEXT("TextLines"), STRUCT_OFFSET(UConsole,TextLines) );
	SetConsolePropertyOffset( BaseClass, TEXT("MsgTime"), STRUCT_OFFSET(UConsole,MsgTime) );
	SetConsolePropertyOffset( BaseClass, TEXT("MsgTickTime"), STRUCT_OFFSET(UConsole,MsgTickTime) );
	SetConsolePropertyOffset( BaseClass, TEXT("MsgText"), STRUCT_OFFSET(UConsole,MsgText) );
	SetConsolePropertyOffset( BaseClass, TEXT("MsgType"), STRUCT_OFFSET(UConsole,MsgType) );
	SetConsolePropertyOffset( BaseClass, TEXT("MsgPlayer"), STRUCT_OFFSET(UConsole,MsgPlayer) );
	SetConsolePropertyOffset( BaseClass, TEXT("MsgTick"), STRUCT_OFFSET(UConsole,MsgTick) );
	SetConsolePropertyOffset( BaseClass, TEXT("BorderSize"), STRUCT_OFFSET(UConsole,BorderSize) );
	SetConsolePropertyOffset( BaseClass, TEXT("ConsoleLines"), STRUCT_OFFSET(UConsole,ConsoleLines) );
	SetConsolePropertyOffset( BaseClass, TEXT("BorderLines"), STRUCT_OFFSET(UConsole,BorderLines) );
	SetConsolePropertyOffset( BaseClass, TEXT("BorderPixels"), STRUCT_OFFSET(UConsole,BorderPixels) );
	SetConsolePropertyOffset( BaseClass, TEXT("ConsolePos"), STRUCT_OFFSET(UConsole,ConsolePos) );
	SetConsolePropertyOffset( BaseClass, TEXT("ConsoleDest"), STRUCT_OFFSET(UConsole,ConsoleDest) );
	SetConsolePropertyOffset( BaseClass, TEXT("FrameX"), STRUCT_OFFSET(UConsole,FrameX) );
	SetConsolePropertyOffset( BaseClass, TEXT("FrameY"), STRUCT_OFFSET(UConsole,FrameY) );
	SetConsolePropertyOffset( BaseClass, TEXT("ConBackground"), STRUCT_OFFSET(UConsole,ConBackground) );
	SetConsolePropertyOffset( BaseClass, TEXT("Border"), STRUCT_OFFSET(UConsole,Border) );
	const INT BoolOffset = STRUCT_OFFSET(UConsole,StartTime) - sizeof(BITFIELD);
	SetConsolePropertyOffset( BaseClass, TEXT("bNoStuff"), BoolOffset );
	SetConsolePropertyOffset( BaseClass, TEXT("bTyping"), BoolOffset );
	SetConsolePropertyOffset( BaseClass, TEXT("bNoDrawWorld"), BoolOffset );
	SetConsolePropertyOffset( BaseClass, TEXT("bTimeDemo"), BoolOffset );
	SetConsolePropertyOffset( BaseClass, TEXT("bStartTimeDemo"), BoolOffset );
	SetConsolePropertyOffset( BaseClass, TEXT("bRestartTimeDemo"), BoolOffset );
	SetConsolePropertyOffset( BaseClass, TEXT("bSaveTimeDemoToFile"), BoolOffset );
	SetConsolePropertyOffset( BaseClass, TEXT("StartTime"), STRUCT_OFFSET(UConsole,StartTime) );
	SetConsolePropertyOffset( BaseClass, TEXT("ExtraTime"), STRUCT_OFFSET(UConsole,ExtraTime) );
	SetConsolePropertyOffset( BaseClass, TEXT("LastFrameTime"), STRUCT_OFFSET(UConsole,LastFrameTime) );
	SetConsolePropertyOffset( BaseClass, TEXT("LastSecondStartTime"), STRUCT_OFFSET(UConsole,LastSecondStartTime) );
	SetConsolePropertyOffset( BaseClass, TEXT("FrameCount"), STRUCT_OFFSET(UConsole,FrameCount) );
	SetConsolePropertyOffset( BaseClass, TEXT("LastSecondFrameCount"), STRUCT_OFFSET(UConsole,LastSecondFrameCount) );
	SetConsolePropertyOffset( BaseClass, TEXT("MinFPS"), STRUCT_OFFSET(UConsole,MinFPS) );
	SetConsolePropertyOffset( BaseClass, TEXT("MaxFPS"), STRUCT_OFFSET(UConsole,MaxFPS) );
	SetConsolePropertyOffset( BaseClass, TEXT("LastSecFPS"), STRUCT_OFFSET(UConsole,LastSecFPS) );
	SetConsolePropertyOffset( BaseClass, TEXT("TimeDemoFont"), STRUCT_OFFSET(UConsole,Font) );
	SetConsolePropertyOffset( BaseClass, TEXT("LoadingMessage"), STRUCT_OFFSET(UConsole,LoadingMessage) );
	SetConsolePropertyOffset( BaseClass, TEXT("SavingMessage"), STRUCT_OFFSET(UConsole,SavingMessage) );
	SetConsolePropertyOffset( BaseClass, TEXT("ConnectingMessage"), STRUCT_OFFSET(UConsole,ConnectingMessage) );
	SetConsolePropertyOffset( BaseClass, TEXT("PausedMessage"), STRUCT_OFFSET(UConsole,PausedMessage) );
	SetConsolePropertyOffset( BaseClass, TEXT("PrecachingMessage"), STRUCT_OFFSET(UConsole,PrecachingMessage) );
	SetConsolePropertyOffset( BaseClass, TEXT("FrameRateText"), STRUCT_OFFSET(UConsole,FrameRateText) );
	SetConsolePropertyOffset( BaseClass, TEXT("AvgText"), STRUCT_OFFSET(UConsole,AvgText) );
	SetConsolePropertyOffset( BaseClass, TEXT("LastSecText"), STRUCT_OFFSET(UConsole,LastSecText) );
	SetConsolePropertyOffset( BaseClass, TEXT("MinText"), STRUCT_OFFSET(UConsole,MinText) );
	SetConsolePropertyOffset( BaseClass, TEXT("MaxText"), STRUCT_OFFSET(UConsole,MaxText) );
	SetConsolePropertyOffset( BaseClass, TEXT("fpsText"), STRUCT_OFFSET(UConsole,fpsText) );
	SetConsolePropertyOffset( BaseClass, TEXT("SecondsText"), STRUCT_OFFSET(UConsole,SecondsText) );
	SetConsolePropertyOffset( BaseClass, TEXT("FramesText"), STRUCT_OFFSET(UConsole,FramesText) );

	if( RebuiltDefaultsPtr )
		BaseClass->Defaults = RebuiltDefaults;

	if( ConsoleClass && ConsoleClass!=BaseClass && ConsoleClass->IsChildOf(BaseClass) )
	{
		if( Delta && ConsoleClass->GetPropertiesSize() >= OldBaseSize )
		{
			const INT OldClassSize = ConsoleClass->GetPropertiesSize();
			const INT NewClassSize = OldClassSize + Delta;
			TArray<BYTE> NewDefaults;
			TArray<BYTE>* NewDefaultsPtr = NULL;
			if( ConsoleClass->Defaults.Num() )
			{
				NewDefaults.AddZeroed( NewClassSize );
				if( BaseClass->Defaults.Num() )
					appMemcpy( &NewDefaults(0), &BaseClass->Defaults(0), Min<INT>( NativeBaseSize, BaseClass->Defaults.Num() ) );
				NewDefaultsPtr = &NewDefaults;
			}
			for( TFieldIterator<UProperty> It(ConsoleClass); It; ++It )
			{
				if( It->GetOuter()==ConsoleClass && It->Offset>=OldBaseSize )
				{
					const INT OldOffset = It->Offset;
					const INT NewOffset = OldOffset + Delta;
					if( NewDefaultsPtr && OldOffset+It->GetSize()<=ConsoleClass->Defaults.Num() && NewOffset>=0 && NewOffset+It->GetSize()<=NewDefaults.Num() )
						It->CopyCompleteValue( &NewDefaults(NewOffset), &ConsoleClass->Defaults(OldOffset) );
					It->Offset = NewOffset;
				}
			}
			ConsoleClass->SetPropertiesSize( NewClassSize );
			if( NewDefaultsPtr )
				ConsoleClass->Defaults = NewDefaults;
			else
				ResizeClassDefaults( ConsoleClass, NewClassSize );
			debugf
			(
				NAME_Warning,
				TEXT("UT99_ANDROID_V171_CONSOLE_SIZE_FIX subclass=%s script=%i native=%i delta=%i"),
				ConsoleClass->GetFullName(),
				OldClassSize,
				ConsoleClass->GetPropertiesSize(),
				Delta
			);
		}
		else if( ConsoleClass->GetPropertiesSize() < NativeBaseSize )
		{
			debugf
			(
				NAME_Warning,
				TEXT("UT99_ANDROID_V171_CONSOLE_SIZE_FIX subclass=%s script=%i native=%i"),
				ConsoleClass->GetFullName(),
				ConsoleClass->GetPropertiesSize(),
				NativeBaseSize
			);
			ConsoleClass->SetPropertiesSize( NativeBaseSize );
			ResizeClassDefaults( ConsoleClass, NativeBaseSize );
		}
	}
#endif
	unguard;
}

/*------------------------------------------------------------------------------
	Console.
------------------------------------------------------------------------------*/

//
// Constructor.
//
UConsole::UConsole()
{}

//
// Init console.
//
void UConsole::_Init( UViewport* InViewport )
{
	guard(UConsole::_Init);
	FixupNativeClassSize( GetClass() );
	VERIFY_CLASS_SIZE(UConsole);

	// Set properties.
	Viewport		= InViewport;
	TopLine			= MAX_LINES-1;
	BorderSize		= 1; 

	// Init scripting.
	InitExecution();

	// Start console log.
	Logf(LocalizeGeneral("Engine",TEXT("Core")));
	Logf(LocalizeGeneral("Copyright",TEXT("Core")));
	Logf(TEXT(" "));
	Logf(TEXT(" "));

	unguard;
}

/*------------------------------------------------------------------------------
	Viewport console output.
------------------------------------------------------------------------------*/

//
// Print a message on the playing screen.
// Time = time to keep message going, or 0=until next message arrives, in 60ths sec
//
void UConsole::Serialize( const TCHAR* Data, EName ThisType )
{
	guard(UConsole::Serialize);
	eventMessage( 0, Data, 0, ThisType );
	unguard;
}

void UConsole::execConsoleCommand( FFrame& Stack, RESULT_DECL )
{
	guardSlow(UConsole::execConsoleCommand);

	P_GET_STR(S);
	P_FINISH;

	*(DWORD*)Result = Viewport->Exec( *S, *this );

	unguardexecSlow;
}
IMPLEMENT_FUNCTION( UConsole, INDEX_NONE, execConsoleCommand );

void UConsole::execSaveTimeDemo( FFrame& Stack, RESULT_DECL )
{
	guard(UConsole::execSaveTimeDemo);
	P_GET_STR(S);
	P_FINISH;
	appSaveStringToFile( S, TEXT("fps.txt"), GFileManager );
	unguardexec;
}
IMPLEMENT_FUNCTION( UConsole, INDEX_NONE, execSaveTimeDemo );

/*------------------------------------------------------------------------------
	Rendering.
------------------------------------------------------------------------------*/

UBOOL UConsole::GetDrawWorld()
{
	guard(UConsole::GetDrawWorld);

	return !bNoDrawWorld;
	unguard;
}

//
// Called before rendering the world view.  Here, the
// Viewport console code can affect the screen's Viewport,
// for example by shrinking the view according to the
// size of the status bar.
//
FSceneNode SavedFrame;
void UConsole::PreRender( FSceneNode* Frame )
{
	guard(UConsole::PreRender);

	// Prevent status redraw due to changing.
	eventTick( Viewport->CurrentTime - Viewport->LastUpdateTime );

	// Save the Viewport.
	SavedFrame = *Frame;

	// Compute new status info.
	BorderLines		= 0;
	BorderPixels	= 0;
	ConsoleLines	= 0;

	// Compute sizing of all visible status bar components.
	if( ConsolePos > 0.0 )
	{
		// Show console.
		ConsoleLines = (INT) Min(ConsolePos * (FLOAT)Frame->Y, (FLOAT)Frame->Y);
	}

	if( BorderSize>=2 )
	{
		// Encroach on screen area.
		FLOAT Fraction = (FLOAT)(BorderSize-1) / (FLOAT)(MAX_BORDER-1);

		BorderLines = (int)Min((FLOAT)Frame->Y * 0.25f * Fraction,(FLOAT)Frame->Y);
		BorderLines = ::Max<INT>(0,BorderLines);
		Frame->Y -= 2 * BorderLines;

		BorderPixels = (int)Min((FLOAT)Frame->X * 0.25f * Fraction,(FLOAT)Frame->X) & ~3;
		Frame->X -= 2 * BorderPixels;
	}

	Frame->XB += BorderPixels;
	Frame->YB += BorderLines;
	Frame->ComputeRenderSize();

	unguard;
}

//
// Refresh the player console on the specified Viewport.  This is called after
// all in-game graphics are drawn in the rendering loop, and it overdraws stuff
// with the status bar, menus, and chat text.
//
void UConsole::PostRender( FSceneNode* Frame )
{
	guard(UConsole::PostRender);
	
	*Frame = SavedFrame;
	FrameX = Frame->X;
	FrameY = Frame->Y;

	unguard;
}

/*------------------------------------------------------------------------------
	The End.
------------------------------------------------------------------------------*/
